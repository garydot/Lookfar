package com.vaguehope.lookfar.servlet;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.vaguehope.lookfar.model.DataStore;

public class UpdatePostServlet extends HttpServlet {

	public static final String CONTEXT = "/update/*";

	private static final long serialVersionUID = 1157053289236694746L;
	private static final Logger LOG = LoggerFactory.getLogger(UpdatePostServlet.class);

	private final DataStore dataStore;

	public UpdatePostServlet (DataStore dataStore) {
		this.dataStore = dataStore;
	}

	@Override
	protected void doPost (HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String node = ServletHelper.extractPathElement(req, resp);
		if (node == null) return;

		HashMap<String, String> data = Maps.newHashMap();
		for (Entry<String, String[]> datum : req.getParameterMap().entrySet()) {
			data.put(datum.getKey(), arrToString(datum.getValue()));
		}
		try {
			this.dataStore.update(node, data);
		}
		catch (SQLException e) {
			LOG.warn("Failed to store data.", e);
			ServletHelper.error(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to store data: " + e.getMessage());
		}
	}

	private static String arrToString (String[] arr) {
		if (arr == null) return "null";
		if (arr.length < 1) return "";
		if (arr.length == 1) return arr[0];
		StringBuilder ret = new StringBuilder(arr[0]);
		for (String a : arr) {
			ret.append(", ").append(a);
		}
		return ret.toString();
	}

}