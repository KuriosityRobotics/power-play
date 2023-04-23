package com.kuriosityrobotics.powerplay.pubsub.bridge;

class PageContent {

	private static final String LOCATION = "<LOCATION>";

	private static final String[] CONTENT_LINES = {
		"<html><head><title>Web Socket Chat</title></head><body>",
		"<script type=\"text/javascript\">",
		"var socket;",
		"",
		"if (!window.WebSocket) {",
		"  window.WebSocket = window.MozWebSocket;",
		"}",
		"",
		"if (window.WebSocket) {",
		"  socket = new WebSocket(\"" + LOCATION + "\");",
		"  socket.onmessage = function(event) {",
		"    var c = document.getElementById('chat');",
		"    c.value = c.value + '\\n' + event.data",
		"  };",
		"  socket.onopen = function(event) {",
		"    var c = document.getElementById('chat');",
		"    c.value = \"Chat opened!\";",
		"  };",
		"  socket.onclose = function(event) {",
		"    var c = document.getElementById('chat');",
		"    c.value = c.value + \"\\nChat closed!\"; ",
		"  };",
		"} else {",
		"  alert(\"Browser does not support Web Socket.\");",
		"}",
		"",
		"function send(message) {",
		"  if (window.WebSocket) {",
		"    if (socket.readyState == WebSocket.OPEN) {",
		"      socket.send(message);",
		"    } else {",
		"      alert(\"The web socket is not open.\");",
		"    }",
		"  }",
		"}",
		"",
		"</script>",
		"",
		"<form onsubmit=\"return false;\">",
		"<textarea id=\"chat\" style=\"width:600px;height:300px;\"></textarea>",
		"<br>",
		"<input type=\"text\" name=\"message\" value=\"\" style=\"width:600px;\"/>",
		"<input type=\"button\" value=\"Send\" onclick=\"send(this.form.message.value);"
				+ " this.form.message.value = '';\"/>",
		"</form>",
		"",
		"</body></html>"
	};

	static String get(String location) {
		StringBuilder sb = new StringBuilder();

		for (String line : CONTENT_LINES) {
			sb.append(line.replace(LOCATION, location));
			sb.append("\r\n");
		}
		return sb.toString();
	}

	private PageContent() {}
}
