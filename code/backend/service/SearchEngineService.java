package com.src.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLEncoder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.html.HtmlParser;
import org.apache.tika.sax.BodyContentHandler;
import org.json.JSONArray;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import com.src.listener.ApplicatonListener;
import com.src.model.HttpResponse;

@Path("/api")
public class SearchEngineService {

	private String searchUrl = "http://localhost:8983/solr/irassignment4_version1/select?q=";
	private String suggestUrl = "http://localhost:8983/solr/irassignment4_version1/suggest?q=";
	private static final String PageRank = "pageRank";
	private static final String spellCheckOn = "on";

	@GET
	@Path("/suggest")
	@Produces({ MediaType.APPLICATION_JSON })
	@Consumes({ MediaType.APPLICATION_JSON })
	public String getSuggestStrings(@QueryParam("q") String query, @Context HttpHeaders headers,
			@Context HttpServletRequest request, @Context HttpServletResponse response) {

		System.out.println("query is" + query);
		HttpResponse httpResponse = makeSuggestRequest(query);
		if (httpResponse != null && httpResponse.getStatusCode() == 200) {
			String json = createSuggestResponseObject(httpResponse.getResponse(), query);
			return json;
		} else {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("data", "Not found");
			return jsonObject.toString();
		}
	}

	private String createSuggestResponseObject(String response, String query) {
		JSONObject res = new JSONObject(response);
		JSONArray suggestions = res.getJSONObject("suggest").getJSONObject("suggest").getJSONObject(query)
				.getJSONArray("suggestions");
		JSONObject data = new JSONObject();
		data.put("data", suggestions);
		return data.toString();
	}

	@GET
	@Path("/search")
	@Produces({ MediaType.APPLICATION_JSON })
	@Consumes({ MediaType.APPLICATION_JSON })
	public String getSystemConfigParamGroups(@QueryParam("q") String query, @QueryParam("sort") String algorithm,
			@QueryParam("spell") String spellCheck, @Context HttpHeaders headers, @Context HttpServletRequest request,
			@Context HttpServletResponse response) {

		System.out.println("query is " + query);
		System.out.println("sort is " + algorithm);
		System.out.println("spell check is " + spellCheck);
		HttpResponse httpResponse = null;

		if (algorithm.equals(PageRank)) {
			httpResponse = makePageRankRequest(query, algorithm);
		} else {
			httpResponse = makeLuceneRequest(query, algorithm);
		}
		if (httpResponse != null && httpResponse.getStatusCode() == 200) {
			JSONObject json = createResponseObject(httpResponse.getResponse(), query);
			if (spellCheck.equals(spellCheckOn)) {
				String correctedWord = getCorrectedWord(query);
				if (correctedWord == null || correctedWord.equalsIgnoreCase(query)) {
					json.put("isCorrected", false);
				} else {
					json.put("isCorrected", true);
					json.put("correctedWord", correctedWord);
				}
			}
			return json.toString();
		} else {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("data", "Not found");
			return jsonObject.toString();
		}
	}

	private static String getCorrectedWord(String query) {
		StringBuffer buffer = new StringBuffer();
		try {
			for (String queryWord : query.split("\\s+")) {
				String crct = SpellCorrector.getInstance().spellCorrect(queryWord);
				buffer.append(crct + " ");
			}
			query = buffer.toString().trim();
			return query;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private HttpResponse makeSuggestRequest(String query) {
		HttpResponse httpResponse = null;
		try {
			String uri = suggestUrl + URLEncoder.encode(query, "UTF-8");
			httpResponse = ApplicatonListener.httpclient.makeHttpGetRequest(uri);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return httpResponse;
	}

	private HttpResponse makePageRankRequest(String query, String algorithm) {
		HttpResponse httpResponse = null;
		try {
			String uri = searchUrl + URLEncoder.encode(query, "UTF-8") + "&sort="
					+ URLEncoder.encode("pageRankFile desc", "UTF-8");
			httpResponse = ApplicatonListener.httpclient.makeHttpGetRequest(uri);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return httpResponse;
	}

	private HttpResponse makeLuceneRequest(String query, String algorithm) {
		HttpResponse httpResponse = null;
		try {
			String uri = searchUrl + URLEncoder.encode(query, "UTF-8");
			httpResponse = ApplicatonListener.httpclient.makeHttpGetRequest(uri);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return httpResponse;

	}

	private JSONObject createResponseObject(String response, String query) {

		JSONObject responseObject = new JSONObject();
		JSONObject res = new JSONObject(response);
		JSONArray documents = res.getJSONObject("response").getJSONArray("docs");
		JSONArray urls = new JSONArray();
		for (int i = 0; i < documents.length(); i++) {
			JSONObject jsonObject = documents.getJSONObject(i);
			JSONObject document = getDetails(jsonObject, query);
			urls.put(document);
		}
		responseObject.put("data", urls);
		responseObject.put("resultsCount", res.getJSONObject("response").getInt("numFound"));
		return responseObject;
	}

	private JSONObject getDetails(JSONObject jsonObject, String query) {
		JSONObject document = new JSONObject();
		String id = jsonObject.getString("id");
		document.put("id", id);
		document.put("title", jsonObject.getJSONArray("title").get(0).toString());
		String url = jsonObject.has("og_url") ? jsonObject.getJSONArray("og_url").get(0).toString()
				: ApplicatonListener.csvFileContens.get(id.substring(id.lastIndexOf("/") + 1));
		System.out.println(url);
		document.put("url", url);
		String snippet = createSnippet(id, query);
		String desc = snippet != "" ? snippet : "N/A";
		document.put("description", desc);
		return document;
	}

	private String createSnippet(String docId, String query) {

		String lowerCaseQuery = query.toLowerCase();
		try {
			String[] lines = readDoc(docId);
			for (String line : lines) {
				if (line.toLowerCase().contains(lowerCaseQuery)) {
					int index = line.toLowerCase().indexOf(lowerCaseQuery);
					return restrictSnippet(line, index);
				}
			}
			for (String line : lines) {
				if (checkForPresence(lowerCaseQuery, line.toLowerCase())) {
					String firstWord = lowerCaseQuery.split("\\s+")[0].trim();
					int index = line.toLowerCase().indexOf(firstWord);
					return restrictSnippet(line, index);
				}
			}

			for (String line : lines) {
				for (String queryTerm : lowerCaseQuery.split("\\s+")) {
					if (line.toLowerCase().contains(queryTerm)) {
						int index = line.toLowerCase().indexOf(queryTerm);
						return restrictSnippet(line, index);
					}
				}
			}

		} catch (IOException | SAXException | TikaException e) {
			return "";
		}
		return "";
	}

	private String restrictSnippet(String line, int index) {

		if (index > 20) {
			index = index - 20;
		}
		int end = Math.min(line.length(), index + 130);
		String result = line.substring(index, end);
		result = result.substring(result.indexOf(" ") + 1);
		return result;
	}

	private boolean checkForPresence(String query, String line) {
		String queryTerms[] = query.split("\\s+");
		for (String term : queryTerms) {
			if (!line.contains(term.trim())) {
				return false;
			}
		}
		return true;
	}

	private String[] readDoc(String docId) throws IOException, SAXException, TikaException {
		File f = new File(docId);
		FileInputStream fis = new FileInputStream(f);
		BodyContentHandler textHandler = new BodyContentHandler(-1);
		Metadata metadata = new Metadata();
		ParseContext parseContext = new ParseContext();
		HtmlParser parser = new HtmlParser();
		parser.parse(fis, textHandler, metadata, parseContext);
		String line = textHandler.toString().replaceAll("\n", "").replaceAll("\t", "").replaceAll("  ", "");
		return line.split("\\.");
	}

	public static void main(String[] args) {
		String s = "sandeep %1$s a good %2$s";
		System.out.println(String.format(s, "is", "boy"));
	}
}
