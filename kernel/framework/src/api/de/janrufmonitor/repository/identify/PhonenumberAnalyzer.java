package de.janrufmonitor.repository.identify;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.repository.identify.Identifier;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.util.io.PathResolver;
import de.janrufmonitor.util.string.StringUtils;

public class PhonenumberAnalyzer {

	private static PhonenumberAnalyzer m_instance;

	private IRuntime m_runtime;
	private Logger m_logger;

	private PhonenumberAnalyzer(IRuntime r) {
		this.m_logger = LogManager.getLogManager().getLogger(
				IJAMConst.DEFAULT_LOGGER);
		this.m_runtime = r;
	}

	public static synchronized PhonenumberAnalyzer getInstance(IRuntime r) {
		if (PhonenumberAnalyzer.m_instance == null) {
			PhonenumberAnalyzer.m_instance = new PhonenumberAnalyzer(r);
		}
		return PhonenumberAnalyzer.m_instance;
	}

	/**
	 * Creates a CLIR phone number object out of a number string
	 * 
	 * @param number
	 *            a phone number as String representation
	 * @return IPhonenumber object with CLIR state
	 */
	public IPhonenumber toClirPhonenumber(String number) {
		if (this.m_logger.isLoggable(Level.INFO)) {
			this.m_logger
					.info("PhonenumberAnalyzer detected RAW call number: ["
							+ number + "]");
			File rawLog = new File(PathResolver.getInstance(getRuntime())
					.getLogDirectory(), "raw-number.log");
			try {
				FileOutputStream fos = new FileOutputStream(rawLog, true);
				fos.write(number.getBytes());
				fos.write(IJAMConst.CRLF.getBytes());
				fos.flush();
				fos.close();
			} catch (FileNotFoundException e) {
				this.m_logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
			} catch (IOException e) {
				this.m_logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
			}
		}

		// check CLIR call
		if (isClired(number)) {
			if (this.m_logger.isLoggable(Level.INFO)) {
				this.m_logger.info("PhonenumberAnalyzer detected CLIR call: ["
						+ number + "]");
			}
			return getRuntime().getCallerFactory().createClirPhonenumber();
		}

		if (this.m_logger.isLoggable(Level.INFO)) {
			this.m_logger
					.info("PhonenumberAnalyzer NOT detected as CLIR number: ["
							+ number + "]");
		}
		return null;
	}

	/**
	 * Creates an internal number object out of a String representation. No MSN
	 * specific truncate options are considered.
	 * 
	 * @param number
	 *            a String representation of a phone number
	 * @return a IPhonenumber object as internal number representation
	 */
	public IPhonenumber toInternalPhonenumber(String number) {
		return toInternalPhonenumber(number, null);
	}

	/**
	 * Creates an internal number object out of a String representation. MSN
	 * specific truncate options are considered.
	 * 
	 * @param number
	 *            a String representation of a phone number
	 * @param msn
	 *            MSN as String for MSN specific truncate options, null to
	 *            ignore MSN options.
	 * @return a IPhonenumber object as internal number representation
	 */
	public IPhonenumber toInternalPhonenumber(String number, String msn) {
		return toInternalPhonenumber(number, msn, true);
	}

	/**
	 * Creates an internal number object out of a String representation. MSN
	 * specific truncate options are considered.
	 * 
	 * @param number
	 *            a String representation of a phone number
	 * @param msn
	 *            MSN as String for MSN specific truncate options, null to
	 *            ignore MSN options.
	 * @param cleanNumber
	 *            if set to true truncate and prefix trim option is executed
	 * @return a IPhonenumber object as internal number representation
	 */
	public IPhonenumber toInternalPhonenumber(String number, String msn,
			boolean cleanNumber) {
		if (this.m_logger.isLoggable(Level.INFO)) {
			this.m_logger
					.info("PhonenumberAnalyzer detected RAW call number: ["
							+ number + "]");
		}
		// check for internal telephone system prefix
		if (cleanNumber && hasInternalPrefix(number)) {
			if (this.m_logger.isLoggable(Level.INFO)) {
				this.m_logger
						.info("PhonenumberAnalyzer detected telephone system prefix: ["
								+ number + "]");
			}
			number = truncateInternalPrefix(number);
			if (this.m_logger.isLoggable(Level.INFO)) {
				this.m_logger
						.info("PhonenumberAnalyzer removed telephone system prefix: ["
								+ number + "]");
			}
		}

		// check for special chars
		if (!containsSpecialChars(number)) {
			int truncate = getInternalTruncate(msn);
			if (cleanNumber && truncate > 0 && number.length()>truncate) {
				number = number.trim().substring(truncate,
						number.trim().length());
				if (this.m_logger.isLoggable(Level.INFO)) {
					this.m_logger
							.info("PhonenumberAnalyzer remove leading digits ["
									+ truncate + "] in number: [" + number
									+ "]");
				}
			}
			if (isInternal(number)) {
				if (this.m_logger.isLoggable(Level.INFO)) {
					this.m_logger
							.info("PhonenumberAnalyzer detected internal number call: ["
									+ number + "]");
				}
				return this.getRuntime().getCallerFactory()
						.createInternalPhonenumber(number);
			}
		} else {
			if (this.m_logger.isLoggable(Level.INFO)) {
				this.m_logger
						.info("PhonenumberAnalyzer detected non-digits in number: ["
								+ number + "]");
			}
			int truncate = getInternalTruncate(msn);
			if (cleanNumber && truncate > 0 && number.length()>truncate) {
				String number1 = number.trim().substring(truncate,
						number.trim().length());
				if (this.m_logger.isLoggable(Level.INFO)) {
					this.m_logger
							.info("PhonenumberAnalyzer remove leading non-digits in number: ["
									+ number1 + "]");
				}
				if (containsSpecialChars(number1)) {
					if (this.m_logger.isLoggable(Level.INFO)) {
						this.m_logger
								.info("PhonenumberAnalyzer detected still non-digits in number: ["
										+ number + "]");
						this.m_logger
								.info("PhonenumberAnalyzer assumes internal number call: ["
										+ number + "]");
					}
					return this.getRuntime().getCallerFactory()
							.createInternalPhonenumber(number);
				}
			} else {
				if (isInternal(number)) {
					if (this.m_logger.isLoggable(Level.INFO)) {
						this.m_logger
								.info("PhonenumberAnalyzer detected internal number call: ["
										+ number + "]");
					}
					return this.getRuntime().getCallerFactory()
							.createInternalPhonenumber(number);
				}
			}
		}

		if (this.m_logger.isLoggable(Level.INFO)) {
			this.m_logger
					.info("PhonenumberAnalyzer NOT detected as internal number: ["
							+ number + "]");
		}
		return null;
	}

	/**
	 * Creates a valid IPhonenumber object out of a number string. This methods
	 * ignores MSN specific settings.
	 * 
	 * @param number
	 *            a raw phone number starting with leading 0
	 * @return a valid IPhonenumber object
	 */
	public IPhonenumber toPhonenumber(String number) {
		return this.toPhonenumber(number, null);
	}

	/**
	 * Creates a valid IPhonenumber object out of a number string. MSN is used
	 * for truncate option and is optional. MSN could be null for default
	 * truncate.
	 * 
	 * @param number
	 *            a raw phone number starting with leading 0
	 * @param msn
	 *            a MSN String
	 * @return a valid IPhonenumber object
	 */
	public IPhonenumber toPhonenumber(String number, String msn) {
		return this.toPhonenumber(number, msn, true);
	}

	/**
	 * Creates a valid IPhonenumber object out of a number string. MSN is used
	 * for truncate option and is optional. MSN could be null for default
	 * truncate.
	 * 
	 * @param number
	 *            a raw phone number starting with leading 0
	 * @param msn
	 *            a MSN String
	 * @param cleanNumber
	 *            if set to true truncate and prefix trim option is executed
	 * @return a valid IPhonenumber object
	 */
	public IPhonenumber toPhonenumber(String number, String msn,
			boolean cleanNumber) {
		if (this.m_logger.isLoggable(Level.INFO)) {
			this.m_logger
					.info("PhonenumberAnalyzer detected RAW call number: ["
							+ number + "]");
		}

		// automatically determine truncate value on inital state
		if (isInitial()) {
			this.calculateTruncate(number);
		}

		// remove hash # at end of number
		if (cleanNumber && number.endsWith("#")) {
			if (this.m_logger.isLoggable(Level.INFO)) {
				this.m_logger
						.info("PhonenumberAnalyzer detected # at end of number: ["
								+ number + "]");
			}
			number = number.substring(0, number.length() - 1);
			if (this.m_logger.isLoggable(Level.INFO)) {
				this.m_logger
						.info("PhonenumberAnalyzer removed # at end of number: ["
								+ number + "]");
			}
		}

		// check for internal telephone system prefix
		if (cleanNumber && hasInternalPrefix(number)) {
			if (this.m_logger.isLoggable(Level.INFO)) {
				this.m_logger
						.info("PhonenumberAnalyzer detected internal prefix: ["
								+ number + "]");
			}
			// 2011/09/22 added synthetic prefix for non area-code numbers.
			number = truncateInternalPrefix(number);
			int truncate = getInternalTruncate(msn);
			if (truncate > 0 && hasMissingAreaCode(number)
					&& !containsSpecialChars(number)) {
				for (int i = 0; i < truncate; i++)
					number = "0" + number;
			}
			if (this.m_logger.isLoggable(Level.INFO)) {
				this.m_logger
						.info("PhonenumberAnalyzer removed internal prefix: ["
								+ number + "]");
			}
		}

		// check for special chars
		if (!containsSpecialChars(number)) {
			if (this.m_logger.isLoggable(Level.INFO)) {
				this.m_logger
						.info("PhonenumberAnalyzer detected regular number call: ["
								+ number + "]");
			}

			int truncate = getTruncate(msn);
			// check for national call number
			if (cleanNumber && truncate > 0) {
				if (this.m_logger.isLoggable(Level.INFO)) {
					this.m_logger
							.info("PhonenumberAnalyzer detected truncate option for MSN ["
									+ msn + "]: " + truncate);
				}
				number = number.trim().substring(truncate,
						number.trim().length());
				if (this.m_logger.isLoggable(Level.INFO)) {
					this.m_logger
							.info("PhonenumberAnalyzer truncated number to ["
									+ number + "]");
				}
			}

			if (!hasMissingAreaCode(number)
					&& number.startsWith("0" + getIntAreaPrefix())) {
				if (this.m_logger.isLoggable(Level.INFO)) {
					this.m_logger
							.info("PhonenumberAnalyzer detected number starts with 0 + international prefix: ["
									+ number + "]");
				}
				number = number.substring(1);
				if (this.m_logger.isLoggable(Level.INFO)) {
					this.m_logger
							.info("PhonenumberAnalyzer removed leading 0 from number: ["
									+ number + "]");
				}
				return getRuntime().getCallerFactory()
						.createPhonenumber(number);
			}

			if (!number.startsWith("0")) { // needed for Fritz!Box variant
											// outgoing calls
				if (this.m_logger.isLoggable(Level.INFO)) {
					this.m_logger
							.info("PhonenumberAnalyzer detected number without leading 0. Assuming local number. ["
									+ number + "]");
				}

				if (hasMissingAreaCode(number)) {
					number = getAreaCode() + number;
					if (this.m_logger.isLoggable(Level.INFO)) {
						this.m_logger
								.info("PhonenumberAnalyzer added areacode to number due to number length. ["
										+ number + "]");
					}
				} else {
					number = "0" + number;
					if (this.m_logger.isLoggable(Level.INFO)) {
						this.m_logger
								.info("PhonenumberAnalyzer added 0 to number. ["
										+ number + "]");
					}
				}
			}
			return getRuntime().getCallerFactory().createPhonenumber(
					(number.startsWith("0") ? number.substring(1) : number));
		}
		if (this.m_logger.isLoggable(Level.INFO)) {
			this.m_logger.info("PhonenumberAnalyzer cannot handle number: ["
					+ number + "]");
			this.m_logger
					.info("PhonenumberAnalyzer assumes internal number call: ["
							+ number + "]");
		}
		return this.getRuntime().getCallerFactory()
				.createInternalPhonenumber(number);
	}

	/**
	 * Creates a valid IPhonenumber object out of a number string. This methods
	 * ignores MSN specific settings. The IPhonenumber object is also being
	 * identified and phone number is split up into 3 parts: international are
	 * code e.g. 49, area code 7261 and phone number 123456789
	 * 
	 * @param number
	 *            a raw phone number starting with leading 0
	 * @return a valid IPhonenumber object which is split up into international
	 *         area code, area code and number
	 */
	public IPhonenumber toIdentifiedPhonenumber(String number) {
		return this.toIdentifiedPhonenumber(number, false);
	}

	/**
	 * Creates a valid IPhonenumber object out of a number string. This methods
	 * ignores MSN specific settings. The IPhonenumber object is also being
	 * identified and phone number is split up into 3 parts: international are
	 * code e.g. 49, area code 7261 and phone number 123456789
	 * 
	 * @param number
	 *            a raw phone number starting with leading 0
	 * @param cleanNumber
	 *            if set to true truncate and prefix trim option is executed
	 * @return a valid IPhonenumber object which is split up into international
	 *         area code, area code and number
	 */
	public IPhonenumber toIdentifiedPhonenumber(String number,
			boolean cleanNumber) {
		String normalized_number = this.normalize(number,
				(!this.isClired(number) && !this.isInternal(number)));
		if (!this.isClired(normalized_number)
				&& !this.isInternal(normalized_number))
			normalized_number = "0" + normalized_number;

		long start = System.currentTimeMillis();
		if (this.m_logger.isLoggable(Level.INFO))
			this.m_logger.info("<---- Begin number identification ---->");

		ICaller c = Identifier.identifyDefault(getRuntime(),
				this.toPhonenumber(normalized_number, null, cleanNumber));
		if (c != null) {
			if (this.m_logger.isLoggable(Level.INFO)) {
				this.m_logger.info("PhonenumberAnalyzer formats number to: ["
						+ c.getPhoneNumber().toString() + "]");
				this.m_logger.info("<---- Finished number identification ("
						+ (System.currentTimeMillis() - start)
						+ " msec.) ---->");
			}
			return c.getPhoneNumber();
		}
		if (this.m_logger.isLoggable(Level.INFO)) {
			this.m_logger.info("PhonenumberAnalyzer did NOT identify number: ["
					+ number + "], normalized: [" + normalized_number + "]");
			this.m_logger.info("<---- Finished number identification ("
					+ (System.currentTimeMillis() - start) + " msec.) ---->");
		}
		return null;
	}

	/**
	 * Formats a string with number information in a callable format. Example:
	 * +4972657110 --> 004972657110 +49 (7165) 7110 --> 004972657110
	 * 
	 * @param phone
	 * @return
	 */
	public String toCallable(String phone) {
		phone = phone.trim();

		phone = StringUtils.replaceString(phone, " ", "");
		phone = StringUtils.replaceString(phone, "/", "");
		phone = StringUtils.replaceString(phone, "(", "");
		phone = StringUtils.replaceString(phone, "(", "");
		phone = StringUtils.replaceString(phone, ")", "");
		phone = StringUtils.replaceString(phone, "-", "");
		// removed 2009/07/02
		// phone = StringUtils.replaceString(phone, "#", "");
		phone = StringUtils.replaceString(phone, ".", "");
		phone = StringUtils.replaceString(phone, "+", "00");

		return phone;
	}

	/**
	 * Normalizes a formatted number to a PIM compliant number string. <br>
	 * <br>
	 * Examples:<br>
	 * Source format: +49 (1234) 567890<br>
	 * Target format: 0491234567890 (international format)<br>
	 * <br>
	 * or<br>
	 * <br>
	 * Source format: (01234) 567890<br>
	 * Target format: 1234567890 (national format)<br>
	 * 
	 * @param phone
	 * @return
	 */
	public String normalize(String phone) {
		return this.normalize(phone, true);
	}

	/**
	 * Returns the international area code code prefix, default: 0
	 * 
	 * @return valid prefix or 0 (zero)
	 */
	public String getIntAreaPrefix() {
		String prefix = this
				.getRuntime()
				.getConfigManagerFactory()
				.getConfigManager()
				.getProperty(IJAMConst.GLOBAL_NAMESPACE,
						IJAMConst.GLOBAL_INTAREA_PREFIX);
		if (this.m_logger.isLoggable(Level.INFO))
			this.m_logger
					.info("PhonenumberAnalyzer configured int area code prefix: ["
							+ prefix + "]");
		return (prefix == null ? "0" : prefix);
	}

	/**
	 * Returns the configured area code
	 * 
	 * @return a valid area code or 0 (zero)
	 */
	public String getAreaCode() {
		String value = this
				.getRuntime()
				.getConfigManagerFactory()
				.getConfigManager()
				.getProperty(IJAMConst.GLOBAL_NAMESPACE,
						IJAMConst.GLOBAL_AREACODE);
		if (this.m_logger.isLoggable(Level.INFO))
			this.m_logger.info("PhonenumberAnalyzer configured area code: ["
					+ value + "]");
		return ((value == null || value.length() == 0) ? "0" : value);
	}

	/**
	 * Checks if a number IPhonenumber object is a CLIR call
	 * 
	 * @param ph
	 *            a IPhonenumber object of a phone number to be checked
	 * @return true if number is a CLIR call
	 */
	public boolean isClired(IPhonenumber pn) {
		return pn.isClired();
	}

	/**
	 * Checks if a number String representation is a CLIR call
	 * 
	 * @param number
	 *            a String representation of a phone number to be checked
	 * @return true if number is a CLIR call
	 */
	public boolean isClired(String number) {
		if (number.trim().length() == 0
				|| number.trim().equalsIgnoreCase(IJAMConst.CLIRED_CALL)
				|| number.trim().indexOf("BLOCKED") > -1
				|| number.trim().indexOf("UNKNOWN") > -1) {
			if (this.m_logger.isLoggable(Level.INFO))
				this.m_logger
						.info("PhonenumberAnalyzer detetced number as CLIR call: ["
								+ number + "]");
			return true;
		}
		return false;
	}

	/**
	 * Checks if a number IPhonenumber object is an internal number, e.g.
	 * intAreaCode is set to IJAMConst.INTERNAL_CALL
	 * 
	 * @param pn
	 *            a Phonenumber object to check
	 * @return true if Phonenumber object representa an internal number
	 */
	public boolean isInternal(IPhonenumber pn) {
		if (pn == null)
			return false;

		if (pn.isClired())
			return false;

		if (!pn.getIntAreaCode().equalsIgnoreCase(IJAMConst.INTERNAL_CALL))
			return false;

		String number = pn.getTelephoneNumber();

		if (number.trim().length() == 0) {
			number = pn.getCallNumber();
		}

		if (number.length() <= getInternalNumberMaxLength()
				|| pn.getIntAreaCode()
						.equalsIgnoreCase(IJAMConst.INTERNAL_CALL)) {
			if (this.m_logger.isLoggable(Level.INFO))
				this.m_logger
						.info("PhonenumberAnalyzer detetced number as internal number: ["
								+ number + "]");
			return true;
		}
		return false;
	}

	/**
	 * Checks if a number String is an internal number, e.g. number string
	 * exceed max length of defined internal numbers
	 * 
	 * @param number
	 *            a String representation of a phone number top check
	 * @return true if number length is smaller then max configured length of
	 *         internal numbers
	 */
	public boolean isInternal(String number) {
		if (number.trim().length() >= 1
				&& number.trim().length() <= getInternalNumberMaxLength()) {
			if (this.m_logger.isLoggable(Level.INFO))
				this.m_logger
						.info("PhonenumberAnalyzer detetced number as internal number: ["
								+ number + "]");
			return true;
		}
		return false;
	}

	/**
	 * Analyzes the behavior of the PhonenumberAnalyzer
	 */
	public void analyze() {
		String[] numbers = new String[] { "", "BLOCKED", IJAMConst.CLIRED_CALL,
				"UNKNOWN", "1", "12", "123", "1234", "030123456789",
				"(030)123456789", "+4930123456789", "004930123456789",
				"004130123456789", "04930123456789", "+49 30 123456789",
				"+49 (30) 123456789", "(030) 123456789", "30123456789" };

		File rawLog = new File(PathResolver.getInstance(getRuntime())
				.getLogDirectory(), "raw-number.log");
		if (rawLog.exists()) {
			try {
				FileInputStream fin = new FileInputStream(rawLog);
				BufferedReader r = new BufferedReader(
						new InputStreamReader(fin));
				List l = new ArrayList();
				String line = null;
				while (r.ready()) {
					line = r.readLine();
					if (line != null && line.trim().length() > 0
							&& !l.contains(line))
						l.add(line);
				}
				r.close();
				fin.close();
				if (l.size() > 0) {
					numbers = new String[l.size()];
					for (int i = 0, s = l.size(); i < s; i++) {
						numbers[i] = (String) l.get(i);
					}
				}
			} catch (FileNotFoundException e) {
				this.m_logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
			} catch (IOException e) {
				this.m_logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
			}
		}

		rawLog = new File(PathResolver.getInstance(getRuntime())
				.getLogDirectory(), "PhonenumberAnalyzer.log");

		try {
			FileOutputStream fos = new FileOutputStream(rawLog, false);
			fos.write(("Created at " + new SimpleDateFormat()
					.format(new Date())).getBytes());
			fos.write(IJAMConst.CRLF.getBytes());
			fos.write(IJAMConst.CRLF.getBytes());
			for (int i = 0; i < numbers.length; i++) {
				fos.write(("<---- Start analyzing number [" + numbers[i] + "] ---->")
						.getBytes());
				fos.write(IJAMConst.CRLF.getBytes());

				fos.write(("Configuration:").getBytes());
				fos.write(IJAMConst.CRLF.getBytes());
				fos.write(("International Area Prefix: ["
						+ PhonenumberAnalyzer.getInstance(this.getRuntime())
								.getIntAreaPrefix() + "], ").getBytes());
				fos.write(("Local Area Code: ["
						+ PhonenumberAnalyzer.getInstance(this.getRuntime())
								.getAreaCode() + "], ").getBytes());
				fos.write(("Global truncate value: ["
						+ PhonenumberAnalyzer.getInstance(this.getRuntime())
								.getTruncate(null) + "], ").getBytes());
				String[] msn = this.getRuntime().getMsnManager().getMsnList();
				for (int j = 0; j < msn.length; j++) {
					fos.write(("MSN ("
							+ msn[j]
							+ ") truncate value: ["
							+ PhonenumberAnalyzer
									.getInstance(this.getRuntime())
									.getTruncate(null) + "], ").getBytes());
				}
				fos.write(("internal prefix: ["
						+ PhonenumberAnalyzer.getInstance(this.getRuntime())
								.getInternalPrefix() + "], ").getBytes());
				fos.write(("max length internal numbers: ["
						+ PhonenumberAnalyzer.getInstance(this.getRuntime())
								.getInternalNumberMaxLength() + "]").getBytes());
				fos.write(IJAMConst.CRLF.getBytes());
				fos.write(IJAMConst.CRLF.getBytes());

				// Start analyze methods
				fos.write(("Calling PhonenumberAnalyzer.toClirPhonenumber("
						+ numbers[i] + "): " + PhonenumberAnalyzer.getInstance(
						this.getRuntime()).toClirPhonenumber(numbers[i]))
						.getBytes());
				fos.write(IJAMConst.CRLF.getBytes());
				fos.write(("Calling PhonenumberAnalyzer.toInternalPhonenumber("
						+ numbers[i] + "): " + PhonenumberAnalyzer.getInstance(
						this.getRuntime()).toInternalPhonenumber(numbers[i]))
						.getBytes());
				fos.write(IJAMConst.CRLF.getBytes());

				for (int j = 0; j < msn.length; j++) {
					fos.write(("Calling PhonenumberAnalyzer.toInternalPhonenumber("
							+ numbers[i] + ", " + msn[j] + "): " + PhonenumberAnalyzer
							.getInstance(this.getRuntime())
							.toInternalPhonenumber(numbers[i], msn[j]))
							.getBytes());
					fos.write(IJAMConst.CRLF.getBytes());
				}

				fos.write(("Calling PhonenumberAnalyzer.toPhonenumber("
						+ numbers[i] + "): " + PhonenumberAnalyzer.getInstance(
						this.getRuntime()).toPhonenumber(numbers[i]))
						.getBytes());
				fos.write(IJAMConst.CRLF.getBytes());
				for (int j = 0; j < msn.length; j++) {
					fos.write(("Calling PhonenumberAnalyzer.toPhonenumber("
							+ numbers[i] + ", " + msn[j] + "): " + PhonenumberAnalyzer
							.getInstance(this.getRuntime()).toPhonenumber(
									numbers[i], msn[j])).getBytes());
					fos.write(IJAMConst.CRLF.getBytes());
				}

				fos.write(("Calling PhonenumberAnalyzer.toIdentifiedPhonenumber("
						+ numbers[i] + "): " + PhonenumberAnalyzer.getInstance(
						this.getRuntime()).toIdentifiedPhonenumber(numbers[i]))
						.getBytes());
				fos.write(IJAMConst.CRLF.getBytes());

				fos.write(("Calling PhonenumberAnalyzer.toCallable("
						+ numbers[i] + "): " + PhonenumberAnalyzer.getInstance(
						this.getRuntime()).toCallable(numbers[i])).getBytes());
				fos.write(IJAMConst.CRLF.getBytes());

				fos.write(("Calling PhonenumberAnalyzer.normalize("
						+ numbers[i] + "): " + PhonenumberAnalyzer
						.getInstance(this.getRuntime()).normalize(numbers[i])
						.toString()).getBytes());
				fos.write(IJAMConst.CRLF.getBytes());

				fos.write(("Calling PhonenumberAnalyzer.containsSpecialChars("
						+ numbers[i] + "): " + Boolean
						.toString(PhonenumberAnalyzer.getInstance(
								this.getRuntime()).containsSpecialChars(
								numbers[i]))).getBytes());
				fos.write(IJAMConst.CRLF.getBytes());

				fos.write(("Calling PhonenumberAnalyzer.isClired(" + numbers[i]
						+ "): " + Boolean.toString(PhonenumberAnalyzer
						.getInstance(this.getRuntime()).isClired(numbers[i])))
						.getBytes());
				fos.write(IJAMConst.CRLF.getBytes());

				fos.write(("Calling PhonenumberAnalyzer.isInternal("
						+ numbers[i] + "): " + Boolean
						.toString(PhonenumberAnalyzer.getInstance(
								this.getRuntime()).isInternal(numbers[i])))
						.getBytes());
				fos.write(IJAMConst.CRLF.getBytes());

				// End analyze methods
				fos.write(("<---- End analyzing number [" + numbers[i] + "] ---->")
						.getBytes());
				fos.write(IJAMConst.CRLF.getBytes());
				fos.write(IJAMConst.CRLF.getBytes());
			}
			fos.flush();
			fos.close();
		} catch (FileNotFoundException e) {
			this.m_logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
		} catch (IOException e) {
			this.m_logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}
	}

	/**
	 * Checks if a number starts with the international prefix
	 * 
	 * @param number
	 *            a String representation of a phone number
	 * @return true is number starts with international prefix
	 */
	// private boolean hasInternationalPrefix(String number) {
	// String pfx = getIntAreaPrefix() +
	// this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE,
	// IJAMConst.GLOBAL_INTAREA);
	// if (this.m_logger.isLoggable(Level.INFO))
	// this.m_logger.info("PhonenumberAnalyzer international prefix is set to: ["+pfx+"], number is: ["+number+"]");
	// return number.startsWith(pfx);
	// }

	/**
	 * Checks wether a number String contains the configured telephone system
	 * prefix
	 * 
	 * @param num
	 *            a String representation of a phone number to check for the
	 *            prefix
	 * @return true if number starts with prefix
	 */
	private boolean hasInternalPrefix(String num) {
		if (num != null && !isClired(num)) {
			String ts_prefix = getInternalPrefix();
			if (ts_prefix.length() > 0) {
				if (this.m_logger.isLoggable(Level.INFO))
					this.m_logger
							.info("PhonenumberAnalyzer detects internal prefix: ["
									+ ts_prefix + ", " + num + "]");
				return num.startsWith(ts_prefix);
			}
		}
		return false;
	}

	/**
	 * Checks whether a phone number as String representation lacks an area
	 * code.
	 * 
	 * @param num
	 *            a String representation of a phone number to check
	 * @return true if area code is missing
	 */
	private boolean hasMissingAreaCode(String num) {
		if (num != null && !isClired(num) && !isInternal(num)) {
			int min_length = this.getInternalNumberMaxLength();
			;
			int max_length = -1;

			String areacodeaddlenth = this
					.getRuntime()
					.getConfigManagerFactory()
					.getConfigManager()
					.getProperty(IJAMConst.GLOBAL_NAMESPACE,
							IJAMConst.GLOBAL_AREACODE_ADD_LENGTH);
			if (areacodeaddlenth != null
					&& areacodeaddlenth.trim().length() > 0) {
				max_length = Integer.parseInt(areacodeaddlenth);
			}
			if (min_length < max_length) {
				return (num.length() > min_length && num.length() <= max_length);
			}
		}
		return false;
	}

	private String getInternalPrefix() {
		String ts_prefix = this
				.getRuntime()
				.getConfigManagerFactory()
				.getConfigManager()
				.getProperty(IJAMConst.GLOBAL_NAMESPACE,
						IJAMConst.GLOBAL_INTERNAL_PREFIX);
		if (ts_prefix != null && ts_prefix.length() > 0) {
			return ts_prefix;
		}
		return "";
	}

	/**
	 * Truncates the international prefix
	 * 
	 * @param number
	 *            a String representation of a phone number
	 * @return truncated number without international prefix
	 */
	// private String truncateInternationalPrefix(String number) {
	// String pfx = getIntAreaPrefix() +
	// this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE,
	// IJAMConst.GLOBAL_INTAREA);
	// if (number.startsWith(pfx)) {
	// if (this.m_logger.isLoggable(Level.INFO))
	// this.m_logger.info("PhonenumberAnalyzer detetced roaming number: ["+number+"]");
	// number = number.substring(pfx.length());
	// }
	// return number;
	// }

	/**
	 * Get the max length of an internal number String. All lengths smaller than
	 * max length are treated as internal numbers.
	 * 
	 * @return int value of max length.
	 */
	private int getInternalNumberMaxLength() {
		String value = this
				.getRuntime()
				.getConfigManagerFactory()
				.getConfigManager()
				.getProperty(IJAMConst.GLOBAL_NAMESPACE,
						IJAMConst.GLOBAL_INTERNAL_LENGTH);
		if (value != null && value.length() > 0) {
			try {
				if (this.m_logger.isLoggable(Level.INFO))
					this.m_logger
							.info("PhonenumberAnalyzer InternalNumber max length set to: ["
									+ Integer.parseInt(value) + "]");
				return Integer.parseInt(value);
			} catch (Exception ex) {
				if (this.m_logger.isLoggable(Level.WARNING))
					this.m_logger.warning(ex.getMessage());
			}
		}
		return 0;
	}

	private int getInternalTruncate(String msn) {
		String trunc = null;
		if (msn != null && msn.length() > 0) {
			trunc = this
					.getRuntime()
					.getConfigManagerFactory()
					.getConfigManager()
					.getProperty(IJAMConst.GLOBAL_NAMESPACE,
							msn + "_" + IJAMConst.GLOBAL_TRUNCATE);
			if (this.m_logger.isLoggable(Level.INFO))
				this.m_logger
						.info("PhonenumberAnalyzer detetced MSN specific truncate value: ["
								+ msn + ", " + trunc + "]");
			if (trunc != null && trunc.length() > 0)
				return Integer.parseInt(trunc);
		}

		trunc = this
				.getRuntime()
				.getConfigManagerFactory()
				.getConfigManager()
				.getProperty(IJAMConst.GLOBAL_NAMESPACE,
						IJAMConst.GLOBAL_INTERNAL_TRUNCATE);
		if (trunc == null || trunc.length() == 0)
			trunc = "0";

		if (this.m_logger.isLoggable(Level.INFO))
			this.m_logger
					.info("PhonenumberAnalyzer detetced internal truncate value: ["
							+ trunc + "]");

		return Integer.parseInt(trunc);
	}

	/**
	 * Gets the count of digits to be truncated based on an MSN specific
	 * configuration
	 * 
	 * @param msn
	 *            a MSN as String representation to check, or null if not
	 *            applicable
	 * @return count of truncatable digits
	 */
	private int getTruncate(String msn) {
		String trunc = null;
		if (msn != null && msn.length() > 0) {
			trunc = this
					.getRuntime()
					.getConfigManagerFactory()
					.getConfigManager()
					.getProperty(IJAMConst.GLOBAL_NAMESPACE,
							msn + "_" + IJAMConst.GLOBAL_TRUNCATE);
			if (this.m_logger.isLoggable(Level.INFO))
				this.m_logger
						.info("PhonenumberAnalyzer detetced MSN specific truncate value: ["
								+ msn + ", " + trunc + "]");
			if (trunc != null && trunc.length() > 0)
				return Integer.parseInt(trunc);
		}

		trunc = this
				.getRuntime()
				.getConfigManagerFactory()
				.getConfigManager()
				.getProperty(IJAMConst.GLOBAL_NAMESPACE,
						IJAMConst.GLOBAL_TRUNCATE);
		if (trunc == null || trunc.length() == 0)
			trunc = "0";

		if (this.m_logger.isLoggable(Level.INFO))
			this.m_logger.info("PhonenumberAnalyzer detetced truncate value: ["
					+ trunc + "]");

		return Integer.parseInt(trunc);
	}

	private IRuntime getRuntime() {
		return this.m_runtime;
	}

	/**
	 * Checks if a number String contains special characters or just contains
	 * out of numbers
	 * 
	 * @param number
	 *            a String representation of a phone number top check
	 * @return true if non-digit numbers or special characters are detected
	 */
	private boolean containsSpecialChars(String number) {
		return !number.matches("[+-]?[0-9]+");
		// Regexp Call numbers
		// ((\+[0-9]{2,4}([ -][0-9]+?[ -]| ?\([0-9]+?\) ?))|(\(0[0-9 ]+?\)
		// ?)|(0[0-9]+? ?( |-|\/) ?))([0-9]+?[ \/-]?)+?[0-9]
	}

	/**
	 * Normalizes a formatted number to a PIM compliant number string. <br>
	 * <br>
	 * Examples:<br>
	 * Source format: +49 (1234) 567890<br>
	 * Target format: 0491234567890 (international format)<br>
	 * <br>
	 * or<br>
	 * <br>
	 * Source format: (01234) 567890<br>
	 * Target format: 1234567890 (national format)<br>
	 * 
	 * remove leading zero as an option.
	 * 
	 * @param phone
	 *            a String representation of a phone number
	 * @param trimLeadingZero
	 *            one leading zero could be remove if set to true
	 * @return a normalized String representation of a phone number
	 */
	private String normalize(String phone, boolean trimLeadingZero) {
		phone = phone.trim();

		if (trimLeadingZero && phone.startsWith("0")) {
			if (this.m_logger.isLoggable(Level.INFO))
				this.m_logger.info("PhonenumberAnalyzer trims leading zero: ["
						+ phone + "]");
			phone = phone.substring(1);
		}

		// added 2009/07/02
		phone = StringUtils.replaceString(phone, "*31#", ""); // remove CLIR
																// symbol in
																// caller number
		phone = StringUtils.replaceString(phone, "#31#", ""); // remove CLIR
																// symbol in
																// caller number
		phone = StringUtils.replaceString(phone, " ", "");
		phone = StringUtils.replaceString(phone, "/", "");
		phone = StringUtils.replaceString(phone, "(0", "");
		phone = StringUtils.replaceString(phone, "(", "");
		phone = StringUtils.replaceString(phone, ")", "");
		phone = StringUtils.replaceString(phone, "-", "");
		phone = StringUtils.replaceString(phone, "#", "");
		phone = StringUtils.replaceString(phone, ".", "");
		phone = StringUtils.replaceString(phone, "+", "0");

		return phone;
	}

	/**
	 * Returns the truncated number without telephone system prefix.
	 * 
	 * @param num
	 *            a String representation of a phone number to truncate the
	 *            prefix from
	 * @return a truncated phone number as String
	 */
	private String truncateInternalPrefix(String num) {
		if (num != null && !isClired(num)) {
			String ts_prefix = this
					.getRuntime()
					.getConfigManagerFactory()
					.getConfigManager()
					.getProperty(IJAMConst.GLOBAL_NAMESPACE,
							IJAMConst.GLOBAL_INTERNAL_PREFIX);
			if (ts_prefix != null && ts_prefix.length() > 0) {
				if (num.startsWith(ts_prefix)) {
					if (this.m_logger.isLoggable(Level.INFO))
						this.m_logger
								.info("PhonenumberAnalyzer truncates telephone system prefix: ["
										+ ts_prefix + ", " + num + "]");
					return num.substring(ts_prefix.length());
				}
			}
		}
		return num;
	}

	private void calculateTruncate(String number) {
		if (!containsSpecialChars(number) && !isInternal(number)
				&& !isClired(number)) {
			// check for telephone system prefix
			int tsp_count = 0;
			String tsp = "";
			while (!number.startsWith("0") && number.length() > tsp_count
					&& tsp_count < 6) {
				tsp += number.substring(0, 1);
				number = number.substring(1);
				tsp_count++;
			}
			if (tsp.length() > 0 && tsp.length() < 5) {
				this.getRuntime()
						.getConfigManagerFactory()
						.getConfigManager()
						.setProperty(IJAMConst.GLOBAL_NAMESPACE,
								IJAMConst.GLOBAL_INTERNAL_PREFIX, tsp);
			}

			// check for truncate digits
			int t_count = -1;
			while (number.startsWith("0") && number.length() > t_count) {
				number = number.substring(1);
				t_count++;
			}
			this.getRuntime()
					.getConfigManagerFactory()
					.getConfigManager()
					.setProperty(IJAMConst.GLOBAL_NAMESPACE,
							IJAMConst.GLOBAL_TRUNCATE,
							Integer.toString((t_count > 0 ? t_count : 0)));
			this.getRuntime()
					.getConfigManagerFactory()
					.getConfigManager()
					.setProperty(IJAMConst.GLOBAL_NAMESPACE,
							IJAMConst.GLOBAL_AREACODE_ADD_LENGTH, "6");
			this.getRuntime()
					.getConfigManagerFactory()
					.getConfigManager()
					.setProperty(IJAMConst.GLOBAL_NAMESPACE,
							IJAMConst.GLOBAL_AUTO_ANALYZE_NUMBER, "false");
			this.getRuntime().getConfigManagerFactory().getConfigManager()
					.saveConfiguration();
		}
	}

	private boolean isInitial() {
		return !this
				.getRuntime()
				.getConfigManagerFactory()
				.getConfigManager()
				.getProperty(IJAMConst.GLOBAL_NAMESPACE,
						IJAMConst.GLOBAL_AUTO_ANALYZE_NUMBER)
				.equalsIgnoreCase("false");
	}

}
