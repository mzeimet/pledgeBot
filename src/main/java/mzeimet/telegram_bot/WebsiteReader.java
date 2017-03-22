package mzeimet.telegram_bot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class WebsiteReader {
	private static final TrustManager[] ALL_TRUSTING_TRUST_MANAGER = new TrustManager[] { new X509TrustManager() {
		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}

		public void checkClientTrusted(X509Certificate[] certs, String authType) {
		}

		public void checkServerTrusted(X509Certificate[] certs, String authType) {
		}
	} };

	private static final HostnameVerifier ALL_TRUSTING_HOSTNAME_VERIFIER = new HostnameVerifier() {
		public boolean verify(String hostname, SSLSession session) {
			return true;
		}
	};

	public static List<String> getPledges(String url) {
		String table = null;
		try {
			HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection();
			// connection.setRequestProperty("User-Agent",
			// "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML,
			// like Gecko) Chrome/23.0.1271.95 Safari/537.11");
			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, ALL_TRUSTING_TRUST_MANAGER, new java.security.SecureRandom());
			SSLSocketFactory sslSocketFactory = sc.getSocketFactory();

			connection.setSSLSocketFactory(sslSocketFactory);

			// Since we may be using a cert with a different name, we need to
			// ignore
			// the hostname as well.
			connection.setHostnameVerifier(ALL_TRUSTING_HOSTNAME_VERIFIER);

			connection.connect();

			BufferedReader r = new BufferedReader(
					new InputStreamReader(connection.getInputStream(), Charset.forName("UTF-8")));

			String line;
			while ((line = r.readLine()) != null) {
				// System.out.println(line);
				if (line.contains("<strong>")) {
					table = line;
					break;
				}
			}
		} catch (MalformedURLException e) {
			throw new RuntimeException("Sorry hab beim Lesen der URL verkackt :(");
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Sorry hab beim Lesen der Webseite verkackt :(");
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (table == null)
			throw new RuntimeException("Sorry, die Webseite enthält anscheinend die Tabelle nicht mehr O.o");

		return parseTable(table);

	}

	private static List<String> parseTable(String table) {
		Pattern pattern = Pattern.compile("<strong>.*?</strong>");
		Matcher matcher = pattern.matcher(table);
		List<String> ret = new ArrayList<String>();
		if (matcher.find()) {
			String tmp = matcher.group();
			ret.add(tmp.substring(8, tmp.length() - 9));
			matcher.find();
			tmp = matcher.group();
			ret.add(tmp.substring(8, tmp.length() - 9));
			matcher.find();
			tmp = matcher.group();
			ret.add(tmp.substring(8, tmp.length() - 9));
		}
		return ret;
	}
}
