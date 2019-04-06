/**
 * 
 */
package com.github.philippn.azureutils;

import static com.github.philippn.azureutils.AzureConstants.RFC_1123_DATE_TIME;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.hash.Hashing.hmacSha256;

import java.io.File;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.Comparator;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.google.common.collect.Lists;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.io.ByteSink;
import com.google.common.io.Files;

/**
 * This program can be used to download a file from an Azure blob container using {@code SharedKey} authorization.
 * @see <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/authorize-with-shared-key">Authorize with Shared Key</a>
 * @author nanz0phi
 */
public class AzureDownloader {

	public static void main(String args[]) throws Exception {
		if (args.length != 3) {
			System.err.println("Syntax: java -jar AzureDownloader.jar <resource-path> <account> <key>");
			System.exit(-1);
		}
		String resourcePath = args[0]; // the path of the resource to download
		String account = args[1]; // the storage acount name
		String key = args[2]; // one of the keys
		String urlString = "https://" + account + ".blob.core.windows.net" + resourcePath;
		HashFunction hashFunction = hmacSha256(Base64.getDecoder().decode(key));
		
		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
			HttpGet httpGet = new HttpGet(urlString);
			signRequest(httpGet, resourcePath, account, hashFunction);
			try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
				System.out.println(response.getStatusLine());
				if (response.getStatusLine().getStatusCode() == 200) {
					int idx = resourcePath.lastIndexOf('/');
					File file = new File(resourcePath.substring(idx + 1)).getCanonicalFile();
					ByteSink sink = Files.asByteSink(file);
					try (OutputStream out = sink.openBufferedStream()) {
						response.getEntity().writeTo(out);
					}
					System.out.println("Wrote " + file);
				} else {
					String responseString = EntityUtils.toString(response.getEntity());
					System.out.println(responseString);
				}
			}
		}
	}

	private static void signRequest(HttpRequestBase request, String resourcePath, String account, 
			HashFunction hashFunction) {
		ZonedDateTime dateTime = ZonedDateTime.now(ZoneId.of("UTC"));
		request.addHeader("x-ms-date", RFC_1123_DATE_TIME.format(dateTime));
		request.addHeader("x-ms-version", "2009-09-19");
		
		String stringToSign = request.getMethod() + "\n"
				+ getHeaderValueOrEmptyString(request, HttpHeaders.CONTENT_TYPE) + "\n"
				+ getHeaderValueOrEmptyString(request, HttpHeaders.CONTENT_LANGUAGE) + "\n"
				+ "" + "\n"
				+ getHeaderValueOrEmptyString(request, HttpHeaders.CONTENT_MD5) + "\n"
				+ getHeaderValueOrEmptyString(request, HttpHeaders.CONTENT_TYPE) + "\n"
				+ "" + "\n"
				+ getHeaderValueOrEmptyString(request, HttpHeaders.IF_MODIFIED_SINCE) + "\n"
				+ getHeaderValueOrEmptyString(request, HttpHeaders.IF_MATCH) + "\n"
				+ getHeaderValueOrEmptyString(request, HttpHeaders.IF_NONE_MATCH) + "\n"
				+ getHeaderValueOrEmptyString(request, HttpHeaders.IF_MODIFIED_SINCE) + "\n"
				+ getHeaderValueOrEmptyString(request, HttpHeaders.RANGE) + "\n"
				+ getCanonicalizedHeaders(request)
				+ getCanonicalizedResource(request, resourcePath, account);
		HashCode hashCode = hashFunction.hashBytes(stringToSign.getBytes(StandardCharsets.UTF_8));
		String signature = Base64.getEncoder().encodeToString(hashCode.asBytes());
		
		request.addHeader("Authorization", "SharedKey " + account + ":" + signature);
		
		for (Header header : request.getAllHeaders()) {
			System.out.println(header.getName() + ": " + header.getValue());
		}
	}

	private static String getHeaderValueOrEmptyString(HttpRequestBase request, String headerName) {
		Header header = request.getFirstHeader(headerName);
		if ((header != null) && !isNullOrEmpty(header.getValue())) {
			return header.getValue();
		}
		return "";
	}

	private static String getCanonicalizedHeaders(HttpRequestBase request) {
		StringBuilder sb = new StringBuilder();
		Lists.newArrayList(request.getAllHeaders()).stream()
				.filter(h -> h.getName().toLowerCase().startsWith("x-ms-"))
				.sorted(Comparator.comparing(Header::getName))
				.forEachOrdered(h -> sb.append(writeHeader(h)));
		return sb.toString();
	}

	private static String getCanonicalizedResource(HttpRequestBase request, String resourcePath, String account) {
		StringBuilder sb = new StringBuilder();
		Lists.newArrayList(request.getAllHeaders()).stream()
				.filter(h -> h.getName().toLowerCase().startsWith("x-ms-"))
				.sorted(Comparator.comparing(Header::getName))
				.forEachOrdered(h -> sb.append(writeHeader(h)));
		return "/" + account + resourcePath;
	}

	private static String writeHeader(Header header) {
		return header.getName().toLowerCase() + ":" + header.getValue() + "\n";
	}
}
