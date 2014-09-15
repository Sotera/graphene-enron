package graphene.enron.web.rest;

import graphene.rest.ws.ExportGraphRS;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import javax.servlet.ServletContext;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.commons.io.IOUtils;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.ApplicationGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExportGraphRSImpl implements ExportGraphRS {

	static Logger logger = LoggerFactory.getLogger(ExportGraphRSImpl.class);

	@Inject
	private ApplicationGlobals globals; // For the ServletContext

	public ExportGraphRSImpl() {
		// constructor
	}

	@Override
	public Response exportGraphAsXML(@QueryParam("fileName") String fileName,
			@QueryParam("fileExt") String fileExt,
			@QueryParam("userName") String userName,
			@QueryParam("timeStamp") String timeStamp, // this is the client
														// timestamp in
														// millisecs as a string
			String graphJSONdata) {

		// ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		// OutputStreamWriter writer = new OutputStreamWriter(outputStream);

		// DEBUG
		logger.debug("exportGraphAsXML: fileName = " + fileName
				+ ", fileExt = " + fileExt);

		/* NOT YET IMPLEMENTED FOR XML */

		return exportGraphAsJSON(fileName, fileExt, userName, timeStamp,
				graphJSONdata);
	}

	public Response exportGraphAsJSON(@QueryParam("fileName") String fileName,
			@QueryParam("fileExt") String fileExt,
			@QueryParam("userName") String userName,
			@QueryParam("timeStamp") String timeStamp, // this is the client
														// timestamp in
														// millisecs as a string
			String graphJSONdata) {

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		OutputStreamWriter writer = new OutputStreamWriter(outputStream);

		// DEBUG
		logger.debug("exportGraphAsJSON: fileName = " + fileName
				+ ", fileExt = " + fileExt + ", graphJSONdata length = "
				+ graphJSONdata.length());

		try {
			writer.write(graphJSONdata);
		} catch (IOException e) {
			logger.error("exportGraphAsJSON: Exception writing JSON");
			e.printStackTrace();
		}

		try {
			writer.close();
			outputStream.flush();
			outputStream.close();
		} catch (java.io.IOException ioe) {
			logger.error("exportGraphAsJSON: I/O Exception when attempting to close output. Details "
					+ ioe.getMessage());
		}

		// Create the file on the Web Server
		File file = null;
		ServletContext servletContext = null;

		try {
			servletContext = globals.getServletContext();
		} catch (Exception se) {
			logger.error("exportGraphAsJSON: ServletContext is null.");
		}

		String path = null;
		String serverfileName = "GraphExport" + "_" + userName + "_"
				+ timeStamp + "_" + fileName + fileExt;

		if (servletContext != null) {
			path = servletContext.getRealPath("/");
		}
		// TODO - get the path from the servlerContext or the request param
		// TODO the file should be placed under the webserver's dir
		if (path == null) {
			// TODO - handle case if the Server is Linux instead of Windows
			path = "C:/Windows/Temp"; // Temp hack
		}

		// DEBUG
		logger.debug("exportGraphAsJSON: file path = " + path);

		try {
			file = new File(path, serverfileName);

			// file.mkdirs();
			FileOutputStream fout = new FileOutputStream(file);
			fout.write(outputStream.toByteArray());
			fout.close();
			String finalPath = file.toURI().toString();
			finalPath = finalPath.replace("file:/", ""); // remove leading

			// DEBUG
			// logger.debug("exportGraphAsJSON: file toURI = " + finalPath);

			ResponseBuilder response = Response.ok(finalPath);
			response.type("text/plain");
			Response responseOut = response.build();
			return responseOut;
		} catch (Exception fe) {
			logger.error("exportGraphAsJSON: Failed to create file for export. Details: "
					+ fe.getLocalizedMessage());
		}
		return null;

	}

	@Override
	public Response getExportedGraph(@QueryParam("filePath") String filePath) {
		// DEBUG
		logger.debug("getExportedGraph: filePath = " + filePath);

		FileInputStream inStream = null;
		String fileContents = null;
		try {
			inStream = new FileInputStream(filePath);
			fileContents = IOUtils.toString(inStream);
			inStream.close();
			// delete the file
			File f = new File(filePath);
			if (!f.delete()) {
				logger.error("getExportedGraph: Failed to delete temporary file: "
						+ filePath);
			}

		} catch (Exception gfe) {
			logger.error("getExportedGraph: Failed to read file. Details: "
					+ gfe.getLocalizedMessage());
		}

		ResponseBuilder response = Response.ok(fileContents);
		// THIS IS KEY:
		// Force the Browser to download/save locally rather than attempt to
		// render it
		response.type("application/x-unknown");
		response.header("Content-Disposition", "attachment; filename=\""
				+ filePath + "\"");
		Response responseOut = response.build();

		return responseOut;
	};

}
