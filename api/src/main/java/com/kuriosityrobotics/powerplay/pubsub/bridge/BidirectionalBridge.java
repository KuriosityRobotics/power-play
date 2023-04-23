package com.kuriosityrobotics.powerplay.pubsub.bridge;

import static com.kuriosityrobotics.powerplay.pubsub.bridge.SerialisationConfig.conf;

import com.google.common.collect.Iterators;
import com.kuriosityrobotics.powerplay.pubsub.Node;
import com.kuriosityrobotics.powerplay.pubsub.Orchestrator;
import com.kuriosityrobotics.powerplay.pubsub.annotation.Hidden;
import com.kuriosityrobotics.powerplay.pubsub.annotation.RunPeriodically;
import com.kuriosityrobotics.powerplay.pubsub.annotation.SubscribedTo;
import com.kuriosityrobotics.powerplay.pubsub.bridge.message.NetworkMessage;
import com.kuriosityrobotics.powerplay.pubsub.bridge.message.NodeInfo;
import com.kuriosityrobotics.powerplay.pubsub.bridge.message.RobotAdvertisement;
import com.kuriosityrobotics.powerplay.pubsub.bridge.message.RobotDetails;

import org.jetbrains.annotations.NotNull;
import org.snf4j.core.SelectorLoop;
import org.snf4j.core.handler.AbstractDatagramHandler;
import org.snf4j.websocket.AbstractWebSocketSessionFactory;
import org.snf4j.websocket.IWebSocketHandler;
import org.snf4j.websocket.WebSocketSession;

import java.io.IOException;
import java.net.BindException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Hidden
public class BidirectionalBridge extends Node {
	private static final Random random = new Random();
	public static int ADVERTISE_PORT = 9999;

	private final InetAddress lanAddress, broadcastAddress;
	private final InetSocketAddress broadcastSocketAddress;

	private final Map<RobotDetails, DataSocketHandler> connections = new ConcurrentHashMap<>();
	private final HandlerFactory handlerFactory;
	private DatagramChannel advertisementChannel;
	private ServerSocketChannel serverSocketChannel;
	protected SelectorLoop loop;
	private byte[] advertisement;

	public SelectorLoop getLoop() {
		return loop;
	}

	public BidirectionalBridge(
		Orchestrator orchestrator) {
		this(orchestrator, new DataSocketHandlerFactory());
	}

	public BidirectionalBridge(
		Orchestrator orchestrator, HandlerFactory handlerFactory) {
		super(orchestrator);

		try {
			this.lanAddress = interfaceAddress().getAddress();
			this.broadcastAddress = getBroadcastAddress();
			this.broadcastSocketAddress = new InetSocketAddress(broadcastAddress, ADVERTISE_PORT);
		} catch (Exception e) {
			close();
			orchestrator.setTimer(1000, () -> orchestrator.startNode("bridge", new BidirectionalBridge(orchestrator, handlerFactory)));
			throw new RuntimeException(e);
		}

		this.handlerFactory = handlerFactory;

		wrapException(
			() -> {
				this.loop = new SelectorLoop();
				var originalExecutor = loop.getExecutor();
				loop.setExecutor(
					task -> {
						originalExecutor.execute(
							() -> {
								try {
									task.run();
								} catch (Throwable e) {
									e.printStackTrace();
									err(e);
									throw e;
								}
							});
					});

				loop.start();

				this.advertisementChannel = openAdvertisementChannel();
				this.serverSocketChannel = setupListener();
				this.advertisement = generateAdvertisement();
			});
	}

	public void distributeMessage(Object datum, String topicName, RobotDetails... targets) {
		var networkMessage =
			SerialisationConfig.conf.asByteArray(
				NetworkMessage.datum(
					orchestrator.robotDetails(), topicName, datum));

		if (targets == null) {
			for (var connection : connections.values())
				connection.send(topicName, networkMessage);
			return;
		}

		assertThat(targets.length > 0, "Targets must either be null or non-empty");
		for (var target : targets)
			connections.get(target).send(topicName, networkMessage);
	}

	@SubscribedTo(topic = ".*", isPattern = true, onlyLocal = true)
	public void distributeMessage(Object datum, String topicName) {
		distributeMessage(datum, topicName, (RobotDetails[]) null);
	}

	@SubscribedTo(topic = "node/started")
	public void onNodeStarted(NodeInfo node, String _topicName, RobotDetails details) {
		if (details.equals(orchestrator.robotDetails())) advertisement = generateAdvertisement();
	}

	private static Inet4Address getBroadcastAddress() {
		try {
			var broadcast = interfaceAddress().getBroadcast();
			return (Inet4Address) broadcast;
		} catch (SocketException e) {
			throw new RuntimeException(e);
		}
	}

	private static Stream<NetworkInterface> interfaces() throws SocketException {
		return StreamSupport.stream(
			Spliterators.spliteratorUnknownSize(Iterators.forEnumeration(NetworkInterface.getNetworkInterfaces()), Spliterator.ORDERED),
			false);

	}

	@NotNull
	private static InterfaceAddress interfaceAddress() throws SocketException {
		return interfaces()
			.filter(
				n -> {
					try {
						return n.isUp();
					} catch (SocketException e) {
						throw new RuntimeException(e);
					}
				})
			.flatMap(n -> n.getInterfaceAddresses().stream())
			.filter(a -> a.getBroadcast() != null)
			.filter(a -> !a.getAddress().isLoopbackAddress())
			.filter(a -> a.getAddress() instanceof Inet4Address || a.getAddress() instanceof Inet6Address)
			.findFirst()
			.get();
	}

	private byte[] generateAdvertisement() {
		return conf.asByteArray(
			new RobotAdvertisement(
				orchestrator.robotDetails(),
				new InetSocketAddress(
					lanAddress, serverSocketChannel.socket().getLocalPort())));
	}

	private ServerSocketChannel setupListener()
		throws IOException, InterruptedException, ExecutionException {
		var serverSocketChannel = ServerSocketChannel.open();

		int bindPort;
		while (true) {
			try {
				bindPort = random.nextInt(65535 - 1024) + 1024;
				var address = new InetSocketAddress("0.0.0.0", bindPort);
				serverSocketChannel.bind(address);
				break;
			} catch (BindException ignored) {
				// try again
			}
		}
		loop.register(
				serverSocketChannel,
				new AbstractWebSocketSessionFactory() {
					@Override
					protected IWebSocketHandler createHandler(SocketChannel channel) {
						var handler =
							handlerFactory.createServer(
								orchestrator,
								wrapException(
									() ->
										new InetSocketAddress(
											lanAddress,
											((InetSocketAddress)
												serverSocketChannel
													.getLocalAddress())
												.getPort())));
						handler.onReceivedRobotDetails =
							details -> {
								connections.put(details, handler);
								handler.onDisconnected = () -> {
									connections.remove(details);
									handler.close();
									orchestrator.dispatch("goodbye", details, "goodbye");
								};
							};
						handler.onDisconnected = () -> {
						};
						return handler;
					}
				})
			.get();

		return serverSocketChannel;
	}

	private DatagramChannel openAdvertisementChannel()
		throws IOException, InterruptedException, ExecutionException {
		var advertisementChannel = DatagramChannel.open(lanAddress instanceof Inet4Address ? StandardProtocolFamily.INET : StandardProtocolFamily.INET6);
		advertisementChannel.configureBlocking(false);
		advertisementChannel.setOption(StandardSocketOptions.SO_BROADCAST, true);
		advertisementChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
		loop.register(advertisementChannel, new AdvertisementHandler()).get();
		advertisementChannel.bind(new InetSocketAddress("0.0.0.0", ADVERTISE_PORT));

		return advertisementChannel;
	}

	@RunPeriodically(maxFrequency = 3)
	void wakeup() {
		connections.forEach((ignored, v) -> v.tryWakeup());
	}

	@RunPeriodically(maxFrequency = 3)
	public synchronized void sendAdvertisement() {
		if (!advertisementChannel.isOpen()) return;
		try {
			advertisementChannel.send(
				ByteBuffer.wrap(advertisement), broadcastSocketAddress);
		} catch (Exception e) {
			close();
			orchestrator.setTimer(1000, () -> orchestrator.startNode("bridge", new BidirectionalBridge(orchestrator, handlerFactory)));
		}
	}

	@Override
	public synchronized void close() {
		super.close();
		wrapException(advertisementChannel::disconnect);
		wrapException(advertisementChannel::close);
		wrapException(serverSocketChannel::close);

		loop.quickStop();
		try {
			orchestrator.assertThat(loop.join(10000));
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} finally {
			loop.dirtyStop();
		}
	}

	public Set<RobotDetails> getConnectedRobots() {
		return Collections.unmodifiableSet(connections.keySet());
	}

	class AdvertisementHandler extends AbstractDatagramHandler {
		private synchronized void read_(byte[] data) throws IOException {
			var _datum = conf.asObject(data);
			if (!orchestrator.assertThat(
				_datum instanceof RobotAdvertisement,
				"UDP packet is not a " + "RobotAdvertisement")) return;

			var datum = (RobotAdvertisement) _datum;
			if (!datum.robotDetails().equals(orchestrator.robotDetails())) {
				if (!orchestrator.robotDetails().shouldBeClient(datum.robotDetails()))
					return;
				connections.computeIfAbsent(
					datum.robotDetails(),
					k -> {
						try {

							var handler =
								handlerFactory.createClient(
									orchestrator,
									datum);
							handler.onDisconnected = () -> {
								connections.remove(datum.robotDetails());
								handler.close();
								orchestrator.dispatch("goodbye", datum.robotDetails(), "goodbye");
							};
							var channel = SocketChannel.open(datum.address());
							channel.configureBlocking(false);
							loop.register(
								channel, new WebSocketSession(handler, true));
							return handler;
						} catch (Exception e) {
							connections.remove(datum.robotDetails());
							err(e);
							throw new RuntimeException();
						}
					});
			}
		}

		@Override
		public void read(byte[] data) {
			try {
				read_(data);
			} catch (IOException e) {
				if (e.getMessage().contains("unknown object tag")) return;

				err(e);
			}
		}

		@Override
		public void read(Object msg) {
			read((byte[]) msg);
		}

		@Override
		public void read(SocketAddress remoteAddress, Object msg) {
			read(msg);
		}
	}
}
