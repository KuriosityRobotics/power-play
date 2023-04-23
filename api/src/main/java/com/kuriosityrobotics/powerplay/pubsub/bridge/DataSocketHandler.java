package com.kuriosityrobotics.powerplay.pubsub.bridge;

import com.kuriosityrobotics.powerplay.pubsub.Orchestrator;
import com.kuriosityrobotics.powerplay.pubsub.bridge.message.NetworkMessage;
import com.kuriosityrobotics.powerplay.pubsub.bridge.message.RobotDetails;
import com.kuriosityrobotics.powerplay.pubsub.bridge.queue.RoundRobinQueue;
import com.kuriosityrobotics.powerplay.pubsub.dynamic.ClassRequest;

import org.snf4j.core.handler.DataEvent;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.handler.SessionIncident;
import org.snf4j.core.session.SessionState;
import org.snf4j.websocket.AbstractWebSocketHandler;
import org.snf4j.websocket.DefaultWebSocketSessionConfig;
import org.snf4j.websocket.IWebSocketSessionConfig;
import org.snf4j.websocket.frame.BinaryFrame;

import java.io.Closeable;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class DataSocketHandler extends AbstractWebSocketHandler implements Closeable {
    public static final int CAPACITY = 32_000_000;

    private final Orchestrator orchestrator;

    private final InetSocketAddress address;
    Consumer<RobotDetails> onReceivedRobotDetails;
    Runnable onDisconnected;

    private final boolean isServer;

    private volatile boolean ready;
    private volatile boolean hasReceivedConnectedRobotDetails;
    private volatile boolean closed;

    private final AtomicBoolean transmissionInProgress = new AtomicBoolean();

    private final RoundRobinQueue queue = new RoundRobinQueue();

    protected DataSocketHandler(
            Orchestrator orchestrator,
            InetSocketAddress address,
            Consumer<RobotDetails> onReceivedRobotDetails,
            Runnable onDisconnected,
            boolean isServer) {
        this.orchestrator = orchestrator;
        this.address = address;
        this.onReceivedRobotDetails = onReceivedRobotDetails;
        this.onDisconnected = onDisconnected;
        this.isServer = isServer;

        ready = false;
        hasReceivedConnectedRobotDetails = false;

        orchestrator.info("DataSocketHandler created at" + address);
    }

    void send(String topicName, byte[] data) {
        queue.add(topicName, data);
        tryWakeup();
    }

    @Override
    public void read(byte[] data) {
        read(SerialisationConfig.conf.asObject(data));
    }

    @Override
    public void read(Object _msg) {
        if (closed) return;
        if (!orchestrator.assertThat(_msg instanceof BinaryFrame, "Message is not a byte array"))
            return;

        try {
            var payload = ((BinaryFrame) _msg).getPayload();
            if (payload.length == 0) {
                orchestrator.warn("Empty payload");
                return;
            }
            var message =
                    (NetworkMessage)
                            SerialisationConfig.conf
                                    .getObjectInput(payload)
                                    .readObject(NetworkMessage.class);

            if (message.robotDetails().equals(orchestrator.robotDetails())) return;

            if (!hasReceivedConnectedRobotDetails) {
                hasReceivedConnectedRobotDetails = true;

                if (onReceivedRobotDetails != null)
                    onReceivedRobotDetails.accept(message.robotDetails());
                orchestrator.info("Connected to " + message.robotDetails() + " at " + address);
            }

            orchestrator.dispatch(message.caption(), message.robotDetails(), message.datum());
        } catch (Throwable e) {
            if (e instanceof ClassNotFoundException
                    || (e.getCause() != null
                    && (e = e.getCause()) instanceof ClassNotFoundException)) {
                var className = e.getMessage().split(" ")[0];
                orchestrator.assertThat(className.startsWith("com.kuriosityrobotics"));

                orchestrator.warn(
                        "Class not found: "
                                + className
                                + ";  attempting to load from other connected robots");
                orchestrator.dispatch("dynamic/class/request", new ClassRequest(className));
                return;
            }

            orchestrator.err(e);
            e.printStackTrace();
        }
    }

    void tryWakeup() {
        if (!queue.hasNext())
            return;
        if (!ready || closed || getSession().getState() != SessionState.OPEN) return;

        if (transmissionInProgress.compareAndSet(false, true)) {
            getSession().writenf(new BinaryFrame(queue.next()));
        }
    }



    @Override
    public void event(DataEvent event, long length) {
        if (event == DataEvent.SENT) {
            if (!ready || closed || getSession().getState() != SessionState.OPEN) {
                transmissionInProgress.set(false);
                return;
            }

            byte[] next;
            if ((next = queue.next()) != null) {
                getSession().writenf(new BinaryFrame(next));
            }
            else
                transmissionInProgress.set(false);
        }
    }

    @Override
    public void event(SessionEvent event) {
        super.event(event);
        switch (event.type()) {
            case SESSION_READY:
                ready = true;

                var helloMessage = SerialisationConfig.conf.asByteArray(NetworkMessage.datum(orchestrator.robotDetails(), "hello", "hello"));
                send("hello", helloMessage);

                break;
            case SESSION_CREATED:
                if (isServer)
                    getSession()
                            .getCodecPipeline()
                            .addFirst(
                                    HttpResponse.HTTP_RESPONDER,
                                    new HttpResponse(address.getHostString()));

                break;

            case SESSION_OPENED:
                orchestrator.info("DataSocketHandler opened");
                break;
            case SESSION_CLOSED:
                orchestrator.info("DataSocketHandler closed");
                break;
            case SESSION_ENDING:
                onDisconnected.run();
                break;
        }
    }

    @Override
    public IWebSocketSessionConfig getConfig() {
        if (isServer)
            return new DefaultWebSocketSessionConfig() {
                {
                    setMaxInBufferCapacity(CAPACITY);
                    setMaxFramePayloadLength(CAPACITY);
                }
            };
        else
            return new DefaultWebSocketSessionConfig(
                    URI.create(
                            "ws://"
                                    + address.getHostString()
                                    + ":"
                                    + address.getPort()
                                    + "/data")) {
                {
                    setMaxInBufferCapacity(CAPACITY);
                    setMaxFramePayloadLength(CAPACITY);
                }
            };
    }

    @Override
    public void close() {
        closed = true;
        if (getSession() != null) getSession().close();
    }

    @Override
    public boolean incident(SessionIncident incident, Throwable t) {
        orchestrator.err("DataSocketHandler incident: " + incident + " " + t);
        return super.incident(incident, t);
    }

    @Override
    public void exception(Throwable t) {
        orchestrator.err("DataSocketHandler exception: " + t);
        super.exception(t);
    }
}
