package net.sf.redmine_mylyn.internal.api.parser;

import java.io.InputStream;
import java.util.Arrays;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;

import net.sf.redmine_mylyn.api.exception.RedmineApiErrorException;
import net.sf.redmine_mylyn.api.exception.RedmineApiRemoteException;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLFilterImpl;

public class JaxbParser<T extends Object> {

	private final static String UNSUPPORTED_NS = "http://redmin-mylyncon.sf.net/schemas/WS-API-2.6";

	private final static String SUPPORTED_NS = "http://redmin-mylyncon.sf.net/api";
	
	protected Class<T> clazz;
	
	protected Class<?>[] classes;
	
	protected JAXBContext ctx;
	
	protected SAXParserFactory parserFactory;
	
	JaxbParser(Class<T> modelClass) {
		this(modelClass, new Class<?>[0]);
	}

	JaxbParser(Class<T> modelClass, Class<?>... requiredClasses) {
		clazz = modelClass;
		classes = Arrays.copyOf(requiredClasses, requiredClasses.length+1);
		classes[requiredClasses.length] = modelClass;
		
		parserFactory = SAXParserFactory.newInstance();
		parserFactory.setNamespaceAware(true);
	}
	
	public T parseInputStream(InputStream stream) throws RedmineApiErrorException {
		try {
			XMLFilterImpl filter = new RedminePluginFilter();
			SAXSource source = new SAXSource(filter, new InputSource(stream));

			return parseInputStream(source);
			
		} catch (ParserConfigurationException e) {
			throw new RedmineApiErrorException("Parsing of InputStream failed (Configuration error)- {0}", e.getMessage(), e);
		} catch (SAXException e) {
			throw new RedmineApiErrorException("Parsing of InputStream failed - {0}", e.getMessage(), e);
		}
	}
	public T parseInputStream(SAXSource source) throws RedmineApiErrorException {
		try {
			Unmarshaller unmarshaller = getUnmarshaller();
			Object obj = unmarshaller.unmarshal(source);
			
			return clazz.cast(obj);
			
		} catch (JAXBException e) {
			if (e.getLinkedException() instanceof RedmineApiRemoteException) {
				throw (RedmineApiRemoteException)e.getLinkedException();
			}
			throw new RedmineApiErrorException("Parsing of InputStream failed", e);
		}
	}
	
	protected Unmarshaller getUnmarshaller() throws JAXBException {
		if (ctx==null) {
			ctx = JAXBContext.newInstance(clazz);
		}
		
		return ctx.createUnmarshaller();
	}

	private class RedminePluginFilter extends XMLFilterImpl {
		
		RedminePluginFilter() throws SAXException, ParserConfigurationException {
			super(parserFactory.newSAXParser().getXMLReader());
		}
		
		@Override
		public void startPrefixMapping(String prefix, String uri) throws SAXException {
			if(uri.equals(UNSUPPORTED_NS)) {
				String msg = "Unsupported Redmine plugin detected, WS-API 2.7 or higher is required.";
				throw new SAXException(msg, new RedmineApiRemoteException(msg));
			}
			
			if(!uri.equals(SUPPORTED_NS)) {
				String msg = "Repository URL doesn't point to a valid Redmine installation with Redmine plugin 2.7 or higher";
				throw new SAXException(msg, new RedmineApiRemoteException(msg));
			}
			
			super.startPrefixMapping(prefix, uri);
		}
	}
}
