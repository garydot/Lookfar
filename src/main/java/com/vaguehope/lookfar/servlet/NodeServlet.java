package com.vaguehope.lookfar.servlet;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import com.vaguehope.lookfar.auth.PasswdGen;
import com.vaguehope.lookfar.model.DataStore;
import com.vaguehope.lookfar.model.Node;
import com.vaguehope.lookfar.model.Update;
import com.vaguehope.lookfar.model.UpdateHelper;
import com.vaguehope.lookfar.util.AsciiTable;
import com.vaguehope.lookfar.util.DateFormatFactory;

public class NodeServlet extends HttpServlet {

	public static final String CONTEXT = "/node/*";

	private static final long serialVersionUID = -6331817712234978364L;
	private static final Logger LOG = LoggerFactory.getLogger(NodeServlet.class);

	private final DataStore dataStore;

	public NodeServlet (DataStore dataStore) {
		this.dataStore = dataStore;
	}

	@Override
	protected void doGet (HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			if (req.getPathInfo() != null && req.getPathInfo().length() > 1) {
				getNodeValue(req, resp);
			}
			else {
				getNodes(resp);
			}
		}
		catch (SQLException e) {
			LOG.warn("Failed to read nodes.", e);
			ServletHelper.error(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to read nodes: " + e.getMessage());
			return;
		}
	}

	private void getNodes (HttpServletResponse resp) throws IOException, ServletException {
		Table<Integer, String, String> table = TreeBasedTable.create();
		try {
			int i = 0;
			for (Node u : this.dataStore.getAllNodes()) {
				Integer row = Integer.valueOf(i++);
				table.put(row, "node", u.getNode());
				table.put(row, "updated", DateFormatFactory.format(u.getUpdated()));
			}
			AsciiTable.printTable(table, new String[] { "node", "updated" }, resp);
		}
		catch (SQLException e) {
			LOG.warn("Failed to read data from store.", e);
			throw new ServletException(e.getMessage());
		}
	}

	private void getNodeValue (HttpServletRequest req, HttpServletResponse resp) throws IOException, SQLException {
		String nodeName = ServletHelper.extractPathElement(req, 1, resp);
		if (nodeName == null) return;

		String keyName = ServletHelper.extractPathElement(req, 2);
		if (keyName == null) {
			UpdateHelper.printUpdates(this.dataStore.getUpdates(nodeName), resp);
		}
		else {
			Update update = this.dataStore.getUpdate(nodeName, keyName);
			if (update != null) {
				resp.getWriter().print(update.getValue());
			}
			else {
				ServletHelper.error(resp, HttpServletResponse.SC_NOT_FOUND, "Failed to find key with name '" + keyName + "' for node with name '" + nodeName + "'.");
				return;
			}
		}
	}

	@Override
	protected void doPut (HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String nodeName = ServletHelper.extractPathElement(req, 1, resp);
		if (nodeName == null) return;

		String pw = PasswdGen.makePasswd();
		String hashpw = BCrypt.hashpw(pw, BCrypt.gensalt());

		try {
			this.dataStore.upsertNode(nodeName, hashpw);
		}
		catch (SQLException e) {
			LOG.warn("Failed to store node.", e);
			ServletHelper.error(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to store node: " + e.getMessage());
			return;
		}

		resp.setStatus(HttpServletResponse.SC_CREATED);
		Table<Integer, String, String> table = TreeBasedTable.create();
		table.put(Integer.valueOf(0), "node", nodeName);
		table.put(Integer.valueOf(0), "pw", pw);
		AsciiTable.printTable(table, new String[] { "node", "pw" }, resp);
	}

	@Override
	protected void doDelete (HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String nodeName = ServletHelper.extractPathElement(req, 1, resp);
		if (nodeName == null) return;

		String keyName = ServletHelper.extractPathElement(req, 2);
		if (keyName == null) {
			deleteNode(resp, nodeName);
		}
		else {
			deleteKey(resp, nodeName, keyName);
		}

	}

	private void deleteNode (HttpServletResponse resp, String nodeName) throws IOException {
		try {
			if (this.dataStore.deleteNode(nodeName) < 1) {
				ServletHelper.error(resp, HttpServletResponse.SC_NOT_FOUND, "Failed to delete node with name '" + nodeName + "'.");
				return;
			}
		}
		catch (SQLException e) {
			LOG.warn("Failed to delete node.", e);
			ServletHelper.error(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to delete node: " + e.getMessage());
			return;
		}
	}

	private void deleteKey (HttpServletResponse resp, String nodeName, String keyName) throws IOException {
		try {
			if (this.dataStore.deleteUpdate(nodeName, keyName) < 1) {
				ServletHelper.error(resp, HttpServletResponse.SC_NOT_FOUND, "Failed to delete key with node '" + nodeName + "' and name '" + nodeName + "'.");
				return;
			}
		}
		catch (SQLException e) {
			LOG.warn("Failed to delete key.", e);
			ServletHelper.error(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to delete key: " + e.getMessage());
			return;
		}
	}

}
