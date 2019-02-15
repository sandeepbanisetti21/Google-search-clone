package com.src.service;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.src.model.HttpResponse;

public class HttpClient {

	private CloseableHttpClient client;

	public void initialize() {
		client = HttpClients.createDefault();
	}

	public HttpResponse makeHttpGetRequest(String url) {
		HttpUriRequest request;
		request = new HttpGet(url);
		CloseableHttpResponse httpStatus = doHttpCall(request);
		return createHttpResponse(httpStatus);
	}

	private HttpResponse createHttpResponse(CloseableHttpResponse httpStatus) {
		HttpResponse response = new HttpResponse();
		response.setStatusCode(httpStatus.getStatusLine().getStatusCode());
		HttpEntity result = httpStatus.getEntity();
		if (result != null) {
			try {
				response.setResponse(EntityUtils.toString(result, "UTF-8"));
			} catch (ParseException | IOException e) {
				e.printStackTrace();
			}
		}
		return response;
	}

	private CloseableHttpResponse doHttpCall(HttpUriRequest request) {
		CloseableHttpResponse httpStatus = null;
		try {
			httpStatus = client.execute(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return httpStatus;
	}
}
