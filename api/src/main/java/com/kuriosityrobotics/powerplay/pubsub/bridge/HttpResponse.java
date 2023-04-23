package com.kuriosityrobotics.powerplay.pubsub.bridge;

import org.snf4j.core.codec.IBaseDecoder;
import org.snf4j.core.codec.IDecoder;
import org.snf4j.core.session.ISession;
import org.snf4j.core.session.IStreamSession;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class HttpResponse implements IDecoder<byte[], byte[]>, IBaseDecoder<byte[], byte[]> {

	static final byte CR = 13;

	static final byte LF = 10;

	static final byte[] CRLF = new byte[] {CR, LF};

	static final byte[] CRLF2 = new byte[] {CR, LF, CR, LF};

	static final String GET = "GET";

	static final String HTTP_VERSION = "HTTP/1.1";

	static final String OK = "200 OK";

	static final String BAD_REQUEST = "400 Bad Request";

	static final String FORBIDDEN = "403 Forbidden";

	static final String NOT_FOUND = "404 Not Found";

	static final String CONTENT_TYPE = "Content-Type: text/html; charset=UTF-8";

	static final String HTTP_RESPONDER = "index-page-decoder";

	static final String[] INDEX_PAGE_ENDPOINTS = new String[] {"/", "/index.htm", "/index.html"};

	private final String host;

	static boolean matches(String value, String[] expectedValues) {
		for (String ev : expectedValues) {
			if (ev.equalsIgnoreCase(value)) {
				return true;
			}
		}
		return false;
	}

	public HttpResponse(String host) {
		this.host = host;
	}

	@Override
	public Class<byte[]> getInboundType() {
		return byte[].class;
	}

	@Override
	public Class<byte[]> getOutboundType() {
		return byte[].class;
	}

	@Override
	public int available(ISession session, ByteBuffer buffer, boolean flipped) {
		ByteBuffer duplicate = buffer.duplicate();
		byte[] data;

		if (!flipped) {
			duplicate.flip();
		}
		data = new byte[duplicate.remaining()];
		duplicate.get(data);
		return available(session, data, 0, data.length);
	}

	@Override
	public int available(ISession session, byte[] buffer, int off, int len) {
		if (len < CRLF2.length) {
			return 0;
		}
		for (int i = off; i < off + len - 3; ++i) {
			boolean found = true;

			for (int j = 0; j < 4; ++j) {
				if (buffer[i + j] != CRLF2[j]) {
					found = false;
					break;
				}
			}
			if (found) {
				return i + 4;
			}
		}
		return 0;
	}

	void response(ISession session, String status, String[] fields, String content) {
		StringBuilder response = new StringBuilder();

		response.append(HTTP_VERSION);
		response.append(" ");
		response.append(status);
		response.append(new String(CRLF));
		if (fields != null && fields.length > 0) {
			for (String field : fields) {
				response.append(field);
				response.append(new String(CRLF));
			}
		}
		response.append(new String(CRLF));
		if (content != null && !content.isEmpty()) {
			response.append(content);
		}
		((IStreamSession) session).writenf(response.toString().getBytes(StandardCharsets.US_ASCII));
		session.close();
	}

	@Override
	public void decode(ISession session, byte[] data, List<byte[]> out) throws Exception {
		BufferedReader in =
				new BufferedReader(new InputStreamReader(new ByteArrayInputStream(data)));

		try {
			String line = in.readLine();

			if (line != null) {
				String[] items = line.split(" ");

				if (items.length > 2 && HTTP_VERSION.equals(items[2])) {
					if (GET.equalsIgnoreCase(items[0])) {
						if (matches(items[1], INDEX_PAGE_ENDPOINTS)) {
							String protocol = "ws://";

							response(
									session,
									OK,
									new String[] {CONTENT_TYPE},
									PageContent.get(protocol + host + "/data"));
						} else if (items[1].equals("/data")) {
							session.getCodecPipeline().remove(HTTP_RESPONDER);
							out.add(data);
						} else {
							response(session, NOT_FOUND, null, null);
						}
					} else {
						response(session, FORBIDDEN, null, null);
					}
				}
			}
		} finally {
			in.close();
		}
	}
}
