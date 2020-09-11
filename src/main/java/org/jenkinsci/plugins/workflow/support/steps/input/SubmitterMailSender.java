package org.jenkinsci.plugins.workflow.support.steps.input;

import hudson.tasks.Mailer;
import hudson.util.Secret;
import jenkins.model.JenkinsLocationConfiguration;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.support.steps.input.i18n.Messages;

import javax.mail.*;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static hudson.Util.fixEmptyAndTrim;

public class SubmitterMailSender {

	private static final Logger LOGGER = Logger.getLogger(SubmitterMailSender.class.getName());
	private static Pattern ADDRESS_PATTERN = Pattern.compile("\\s*([^<]*)<([^>]+)>\\s*");

	private String charset;
	private String smtpHost;
	private String adminAddress;

	private boolean useSMTPAuth;
	private String smtpAuthUserName;
	private Secret smtpAuthPasswordSecret;

	private boolean useSsl;
	private String smtpPort;

	private String recipients;
	private String replyToAddress;
	
	private String message;
	private String timeout; //It looks like:24-HOURS or 30-SECONDS
	private int buildNumber;
	private String pipelineName;
	
	private String website;
	private String serviceline;
	private String mailbox;

	public SubmitterMailSender(String recipients, String pipelineName, int buildNumber, String message, String timeout, String website, String serviceline, String mailbox) {
		this.pipelineName = pipelineName;
		this.buildNumber = buildNumber;
		this.recipients = recipients;
		this.message = message;
		this.website = website;
		this.serviceline = serviceline;
		this.mailbox = mailbox;
		
		String[] strArr = timeout.split("-");
		if (strArr[1].trim().equals("HOURS")) {
			this.timeout = strArr[0] + "小时";
		}
		if (strArr[1].trim().equals("MINUTES")) {
			this.timeout = strArr[0] + "分钟";
		}

		JenkinsLocationConfiguration jlc = JenkinsLocationConfiguration.get();
		if (jlc != null) {
			charset = Mailer.descriptor().getCharset();
			smtpHost = Mailer.descriptor().getSmtpServer();
			adminAddress = jlc.getAdminAddress();

			smtpAuthUserName = Mailer.descriptor().getSmtpAuthUserName();
			smtpAuthPasswordSecret = Mailer.descriptor().getSmtpAuthPasswordSecret();

			useSsl = Mailer.descriptor().getUseSsl();
			smtpPort = Mailer.descriptor().getSmtpPort();
			replyToAddress = Mailer.descriptor().getReplyToAddress();

			if (smtpAuthUserName != null) {
				useSMTPAuth = true;
			}
		}
	}

	public void send() {
		if (!useSMTPAuth) {
			smtpAuthUserName = null;
			smtpAuthPasswordSecret = null;
		}

		MimeMessage msg = new MimeMessage(createSession(smtpHost, smtpPort, useSsl, smtpAuthUserName, smtpAuthPasswordSecret));
		try {
			msg.setSubject(Messages.Mailer_Subject(), charset);
			msg.setText(Messages.Mailer_Content(pipelineName, buildNumber, message, timeout, website, serviceline, mailbox), charset);
			msg.setFrom(stringToAddress(adminAddress, charset));
			if (StringUtils.isNotBlank(replyToAddress)) {
				msg.setReplyTo(new Address[] { stringToAddress(replyToAddress, charset) });
			}
			msg.setSentDate(new Date());
			
			String[] recipientArr = recipients.split(",");
			for (String str: recipientArr) {
				msg.setRecipient(Message.RecipientType.TO, stringToAddress(str.trim(), charset));

				Transport.send(msg);
			}		
		} catch (MessagingException | UnsupportedEncodingException e) {
			LOGGER.log(Level.WARNING, "Failed to send eamil to submitter " + e);
		}
	}

	private Address stringToAddress(String strAddress, String charset) throws AddressException, UnsupportedEncodingException {
		Matcher m = ADDRESS_PATTERN.matcher(strAddress);
		if (!m.matches()) {
			return new InternetAddress(strAddress);
		}

		String personal = m.group(1);
		String address = m.group(2);
		return new InternetAddress(address, personal, charset);
	}

	private Session createSession(String smtpHost, String smtpPort, boolean useSsl, String smtpAuthUserName, Secret smtpAuthPassword) {
		smtpPort = fixEmptyAndTrim(smtpPort);
		smtpAuthUserName = fixEmptyAndTrim(smtpAuthUserName);

		Properties props = new Properties(System.getProperties());
		if (fixEmptyAndTrim(smtpHost) != null)
			props.put("mail.smtp.host", smtpHost);
		if (smtpPort != null) {
			props.put("mail.smtp.port", smtpPort);
		}
		if (useSsl) {
			if (props.getProperty("mail.smtp.socketFactory.port") == null) {
				String port = smtpPort == null ? "465" : smtpPort;
				props.put("mail.smtp.port", port);
				props.put("mail.smtp.socketFactory.port", port);
			}
			if (props.getProperty("mail.smtp.socketFactory.class") == null) {
				props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
			}
			props.put("mail.smtp.socketFactory.fallback", "false");
		}

		if (smtpAuthUserName != null) {
			props.put("mail.smtp.auth", "true");
		}
		props.put("mail.smtp.timeout", "60000");
		props.put("mail.smtp.connectiontimeout", "60000");

		return Session.getInstance(props, getAuthenticator(smtpAuthUserName, Secret.toString(smtpAuthPassword)));
	}

	private Authenticator getAuthenticator(final String smtpAuthUserName, final String smtpAuthPassword) {
		if (smtpAuthUserName == null) {
			return null;
		}

		return new Authenticator() {

			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(smtpAuthUserName, smtpAuthPassword);
			}
		};
	}

}
