package com.vaguehope.lookfar.android.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.http.client.HttpResponseException;

import android.util.Base64;

public class HttpHelper {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static final int HTTP_CONNECT_TIMEOUT_SECONDS = 20;
	public static final int HTTP_READ_TIMEOUT_SECONDS = 60;

	private static final String HEADER_AUTHORISATION = "Authorization";

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public interface HttpStreamHandler<T extends Exception> {

		public void handleStream (InputStream is, int contentLength) throws IOException, T;

	}

	public interface HttpCreds {

		public String getUser ();

		public String getPass ();

	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static <T extends Exception> String getUrlContent (String sUrl, HttpStreamHandler<T> streamHandler, HttpCreds creds) throws IOException, T {
		return getUrlContent(sUrl, null, null, null, streamHandler, creds);
	}

	public static String getUrlContent (String sUrl, String httpRequestMethod, String encodedData, String contentType, HttpCreds creds) throws IOException {
		return getUrlContent(sUrl, httpRequestMethod, encodedData, contentType, (HttpStreamHandler<RuntimeException>) null, creds);
	}

	public static <T extends Exception> String getUrlContent (String sUrl, String httpRequestMethod, String encodedData, String contentType, HttpStreamHandler<T> streamHandler, HttpCreds creds) throws IOException, T {
		URL url = new URL(sUrl);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setUseCaches(false);
		connection.setRequestMethod(httpRequestMethod != null ? httpRequestMethod : "GET");
		connection.setConnectTimeout(HTTP_CONNECT_TIMEOUT_SECONDS * 1000);
		connection.setReadTimeout(HTTP_READ_TIMEOUT_SECONDS * 1000);
		connection.setRequestProperty(HEADER_AUTHORISATION, authHeader(creds));

		if (encodedData != null) {
			if (contentType != null) connection.setRequestProperty("Content-Type", contentType);
			connection.setDoOutput(true);
			OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
			try {
				out.write(encodedData);
				out.flush();
			}
			finally {
				out.close();
			}
		}

		StringBuilder sb = null;
		InputStream is = null;
		try {
			int responseCode = connection.getResponseCode();
			if (responseCode >= 400) {
				sb = new StringBuilder();
				buildString(connection.getErrorStream(), sb);
				throw new HttpResponseException(responseCode, sb.toString());
			}

			is = connection.getInputStream();

			if (streamHandler != null) {
				streamHandler.handleStream(is, connection.getContentLength());
			}
			else {
				sb = new StringBuilder();
				buildString(is, sb);
			}
		}
		finally {
			if (is != null) is.close();
		}

		return sb == null ? null : sb.toString();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static String authHeader (HttpCreds creds) {
		String raw = creds.getUser() + ":" + creds.getPass();
		String enc = Base64.encodeToString(raw.getBytes(), Base64.NO_WRAP);
		return "Basic " + enc;
	}

	private static void buildString (InputStream is, StringBuilder sb) throws IOException {
		BufferedReader rd = new BufferedReader(new InputStreamReader(is));
		String line;
		while ((line = rd.readLine()) != null) {
			sb.append(line);
			sb.append("\n");
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
