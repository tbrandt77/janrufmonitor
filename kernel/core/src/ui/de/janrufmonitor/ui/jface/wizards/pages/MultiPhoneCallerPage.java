package de.janrufmonitor.ui.jface.wizards.pages;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

import de.janrufmonitor.framework.IAttribute;
import de.janrufmonitor.framework.IAttributeMap;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IMultiPhoneCaller;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.repository.identify.PhonenumberAnalyzer;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.jface.application.editor.Editor;
import de.janrufmonitor.ui.swt.DisplayManager;
import de.janrufmonitor.ui.swt.SWTImageManager;
import de.janrufmonitor.util.formatter.Formatter;
import de.janrufmonitor.util.io.ImageHandler;
import de.janrufmonitor.util.io.OSUtils;
import de.janrufmonitor.util.io.PathResolver;
import de.janrufmonitor.util.io.Stream;
import de.janrufmonitor.util.string.StringUtils;

public class MultiPhoneCallerPage extends AbstractPage {

	class NumberView {
 
		IPhonenumber m_n;

		IAttributeMap m_m;

		TabFolder m_c;

		String m_title;

		public NumberView(IPhonenumber n, IAttributeMap m, TabFolder c, String s) {
			m_n = n;
			m_c = c;
			m_m = getRuntime().getCallerFactory().createAttributeMap();
			IAttribute a = null;
			Iterator i = m.iterator();
			while (i.hasNext()) {
				a = (IAttribute) i.next();

				if (n.getTelephoneNumber().trim().length()>0 && a.getName().endsWith(n.getTelephoneNumber())) {
					
					m_m.add(a);

				}

			}

			if (m_m.contains(IJAMConst.ATTRIBUTE_NAME_NUMBER_TYPE
					+ n.getTelephoneNumber())) {
				a = m_m.get(IJAMConst.ATTRIBUTE_NAME_NUMBER_TYPE
						+ n.getTelephoneNumber());
				m_title = s
						+ m_i18n
								.getString(
										"ui.jface.application.editor.rendering.NumberType",
										a.getValue(), "label", m_language);
			}

			if (m_title == null)
				m_title = s;
		}

		public IPhonenumber getPhoneNumber() {
			return m_n;
		}

		public IAttributeMap getAttributes() {
			IAttributeMap localMap = getRuntime().getCallerFactory().createAttributeMap();
			Iterator i = m_m.iterator();
			IAttribute a = null;
			while (i.hasNext()) {
				a = (IAttribute) i.next();
				if (a!=null && !a.getName().startsWith(IJAMConst.ATTRIBUTE_NAME_NUMBER_TYPE) && !a.getName().endsWith(this.m_n.getTelephoneNumber()) && a.getName().indexOf("-")>-1) {
					String name = a.getName();
					name = name.substring(0, name.lastIndexOf("-")+1);	
					name += this.m_n.getTelephoneNumber();
					a.setName(name);
					localMap.add(a);
				} else {
					localMap.add(a);
				}
			}
			return localMap;
		}

		public void render() {
			TabItem item = new TabItem(m_c, SWT.NONE);
			item.setText(m_title);

			Group phoneGroup = new Group(m_c, SWT.SHADOW_ETCHED_IN);
			phoneGroup.setLayout(new GridLayout(2, true));
			phoneGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			final Label ial = new Label(phoneGroup, SWT.LEFT);
			ial.setText(m_i18n.getString(getNamespace(), "number", "label",
					m_language));
			ial.setToolTipText(m_i18n.getString(getNamespace(),
					"tooltipprefix", "label", m_language)
					+ IJAMConst.GLOBAL_VARIABLE_CALLERNUMBER);

			GridData gd = new GridData();
			gd.widthHint = 300;
			
			new Label(phoneGroup, SWT.LEFT);

			final Text number = new Text(phoneGroup, SWT.BORDER);
			number.setLayoutData(gd);
			number.setSize(number.getSize().x, 50);
			number.setText(getPhoneNumber().getIntAreaCode());
			number.setEditable(!m_numberReadonly);

			IAttribute type = m_m.get(IJAMConst.ATTRIBUTE_NAME_NUMBER_TYPE
					+ this.m_n.getTelephoneNumber());

			final Combo typeCombo = new Combo(phoneGroup, SWT.READ_ONLY);

			typeCombo.select(0);

			String[] types = new String[] { "",
					IJAMConst.ATTRIBUTE_VALUE_LANDLINE_TYPE,
					IJAMConst.ATTRIBUTE_VALUE_MOBILE_TYPE,
					IJAMConst.ATTRIBUTE_VALUE_FAX_TYPE };

			String[] typesText = new String[] {
					"",
					m_i18n.getString(
							"ui.jface.application.editor.rendering.NumberType",
							IJAMConst.ATTRIBUTE_VALUE_LANDLINE_TYPE, "label",
							m_language),
					m_i18n.getString(
							"ui.jface.application.editor.rendering.NumberType",
							IJAMConst.ATTRIBUTE_VALUE_MOBILE_TYPE, "label",
							m_language),
					m_i18n.getString(
							"ui.jface.application.editor.rendering.NumberType",
							IJAMConst.ATTRIBUTE_VALUE_FAX_TYPE, "label",
							m_language) };

			int select = 0;
			for (int i = 0; i < types.length; i++) {
				typeCombo.setData(typesText[i], types[i]);
				if (type != null && types[i].equalsIgnoreCase(type.getValue())) {
					select = i;
				}
			}
			typeCombo.setItems(typesText);
			typeCombo.select(select);
			typeCombo.setEnabled(!m_numberReadonly);

			if (PhonenumberAnalyzer.getInstance(getRuntime()).isInternal(this.getPhoneNumber())) {
				number.setText(this.getPhoneNumber().getCallNumber());
//				number.setText(this.getPhoneNumber().getCallNumber().substring(
//						0,
//						Math.min(
//								this.getPhoneNumber().getCallNumber().length(),
//								PhonenumberInfo.maxInternalNumberLength())));
			} else {
				number.setText(Formatter.getInstance(getRuntime()).parse(
						IJAMConst.GLOBAL_VARIABLE_CALLERNUMBER,
						getPhoneNumber()));
			}

			typeCombo.setEnabled(!m_numberReadonly
					&& !PhonenumberAnalyzer.getInstance(getRuntime()).isInternal(number.getText())
					&& !PhonenumberAnalyzer.getInstance(getRuntime()).isClired(number.getText()));

			// changed 2015/07/12: 
			// number.getText().length() > PhonenumberAnalyzer.getInstance(getRuntime()).getInternalNumberMaxLength());
			
			// Add the handler to update the name based on input
			typeCombo.addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent event) {
					String scat = typeCombo.getItem(typeCombo
							.getSelectionIndex());
					scat = (String) typeCombo.getData(scat);
					m_m.add((getRuntime().getCallerFactory().createAttribute(
							IJAMConst.ATTRIBUTE_NAME_NUMBER_TYPE
									+ getPhoneNumber().getTelephoneNumber(),
							scat)));
					if (scat == null || scat.trim().length() == 0) {
						m_m.remove(IJAMConst.ATTRIBUTE_NAME_NUMBER_TYPE
								+ m_n.getTelephoneNumber());
					}

					setPageComplete(isComplete());
				}
			});

			number.addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent event) {
					IPhonenumber pn = PhonenumberAnalyzer.getInstance(getRuntime()).toInternalPhonenumber(PhonenumberAnalyzer.getInstance(getRuntime()).normalize(number.getText().trim()), null);
					if (pn==null) pn = PhonenumberAnalyzer.getInstance(getRuntime()).toPhonenumber(PhonenumberAnalyzer.getInstance(getRuntime()).normalize(number.getText().trim()), null);
					if (!PhonenumberAnalyzer.getInstance(getRuntime()).isInternal(pn) && !PhonenumberAnalyzer.getInstance(getRuntime()).isClired(pn))
						typeCombo.setEnabled(!m_numberReadonly && true);
					else {
						typeCombo.setEnabled(!m_numberReadonly && false);
						typeCombo.select(0);
					}

					if (number.getText().trim().length()==0) {
						m_n = getRuntime().getCallerFactory().createPhonenumber(true);
					}
					
					setPageComplete(isComplete());
				}
			});

			number.addFocusListener(new FocusAdapter() {
				public void focusGained(org.eclipse.swt.events.FocusEvent e) {
					number.selectAll();
				}

				public void focusLost(org.eclipse.swt.events.FocusEvent e) {
					// check the number input
					if (number.getText().trim().length() == 0){
						setPageComplete(isComplete());
						return;
					}

					IPhonenumber pn = PhonenumberAnalyzer.getInstance(getRuntime()).toInternalPhonenumber(PhonenumberAnalyzer.getInstance(getRuntime()).normalize(number.getText().trim()), null);
					if (pn==null) pn = PhonenumberAnalyzer.getInstance(getRuntime()).toPhonenumber(PhonenumberAnalyzer.getInstance(getRuntime()).normalize(number.getText().trim()), null);
					if (PhonenumberAnalyzer.getInstance(getRuntime()).isInternal(pn)) {
						m_n = getRuntime().getCallerFactory()
								.createInternalPhonenumber(number.getText().trim());

						setPageComplete(isComplete());
						return;
					}

					m_n = PhonenumberAnalyzer.getInstance(getRuntime()).toIdentifiedPhonenumber(number.getText().trim());
					if (m_n!=null) {
						number
								.setText(Formatter
										.getInstance(getRuntime())
										.parse(
												IJAMConst.GLOBAL_VARIABLE_CALLERNUMBER,
												getPhoneNumber()));
					} else {
						if (PhonenumberAnalyzer.getInstance(getRuntime()).isInternal(PhonenumberAnalyzer.getInstance(getRuntime()).normalize(number.getText().trim()))) {
							m_n = PhonenumberAnalyzer.getInstance(getRuntime()).toInternalPhonenumber(PhonenumberAnalyzer.getInstance(getRuntime()).normalize(number.getText().trim()));
							number.setText(Formatter.getInstance(
									getRuntime()).parse(
									IJAMConst.GLOBAL_VARIABLE_CALLERNUMBER,
									getPhoneNumber()));
						}
					}
					
					setPageComplete(isComplete());
				}
			});

			item.setControl(phoneGroup);
			item.setData(this);
		}

	}

	private String NAMESPACE = "ui.jface.wizards.pages.CallerPage";

	private IRuntime m_runtime;

	private IMultiPhoneCaller m_caller;

	private boolean m_callerReadonly;

	private boolean m_numberReadonly;

	private boolean m_allowClirEdit;
	
	private boolean m_useGRAVATAR;

	private TabFolder tabFolder;

	public MultiPhoneCallerPage(ICaller c, boolean callerReadonly,
			boolean numberReadonly, boolean allowClirEdit) {
		super(MultiPhoneCallerPage.class.getName());
		if (!(c instanceof IMultiPhoneCaller))
			c = getRuntime().getCallerFactory().toMultiPhoneCaller(c);
		this.m_caller = (IMultiPhoneCaller) c;
		this.m_callerReadonly = callerReadonly;
		this.m_numberReadonly = numberReadonly;
		this.m_allowClirEdit = allowClirEdit;
		setTitle(this.m_i18n.getString(getNamespace(), "title", "label",
				this.m_language));
		setDescription(this.m_i18n.getString(getNamespace(), "description",
				"label", this.m_language));
	}

	protected IRuntime getRuntime() {
		if (this.m_runtime == null) {
			this.m_runtime = PIMRuntime.getInstance();
		}
		return this.m_runtime;
	}

	public String getNamespace() {
		return NAMESPACE;
	}

	public ICaller getResult() {
		// get Phones
		List phones = new ArrayList(tabFolder.getItemCount());
		for (int i = 0, j = tabFolder.getItemCount(); i < j; i++) {
			phones.add(((NumberView) tabFolder.getItem(i).getData())
					.getPhoneNumber());
			this.m_caller.getAttributes().addAll(
					((NumberView) tabFolder.getItem(i).getData())
							.getAttributes());
		}		
		this.m_caller.setPhonenumbers(phones);
		
		// check for GRAVATAR image
		this.m_caller.getAttributes().remove(IJAMConst.ATTRIBUTE_NAME_GRAVATAR);
		if (m_useGRAVATAR && this.m_caller.getAttributes().contains(IJAMConst.ATTRIBUTE_NAME_EMAIL)) {
			String email = this.m_caller.getAttribute(IJAMConst.ATTRIBUTE_NAME_EMAIL).getValue();
			if (email != null && email.length()>0) {	
				File dir = new File(PathResolver.getInstance().getPhotoDirectory());
				if (!dir.exists()) dir.mkdirs();
				
				File img = new File(dir, this.m_caller.getPhoneNumber().getTelephoneNumber()+".jpg");
				try {
					email = StringUtils.toMD5Hex(email);
					String gravatar = "http://www.gravatar.com/avatar/"+email+".jpg?d=404&s=120";
					URL url = new URL(gravatar);
					URLConnection c = url.openConnection();
					
					StringBuffer agent = new StringBuffer();
					agent.append("jAnrufmonitor Update Manager ");
					agent.append(IJAMConst.VERSION_DISPLAY);
					
					c.setDoInput(true);
					c.setRequestProperty(
						"User-Agent",
						agent.toString());
					c.connect();

					Object o = url.openStream();
					if (o instanceof InputStream) {
						this.m_caller.getAttributes().add(getRuntime().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_GRAVATAR, gravatar));
						BufferedInputStream bin = new BufferedInputStream((InputStream) o);
						FileOutputStream fos = new FileOutputStream(img);
						Stream.copy(bin, fos, true);
						this.m_caller.getAttributes().add(getRuntime().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_IMAGEPATH, img.getAbsolutePath()));
					}
				} catch (FileNotFoundException e) {
					m_logger.info("No GRAVATAR found: "+e.getMessage());
				} catch (IOException e) {
					m_logger.info("IOException: "+e.getMessage());
				}
			}
		}
		
		return this.m_caller;
	}

	public void createControl(Composite parent) {
		Composite c = new Composite(parent, SWT.NONE);
		c.setLayout(new GridLayout(2, false));

		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 300;
		gd.verticalAlignment = GridData.VERTICAL_ALIGN_BEGINNING;
		c.setLayoutData(gd);

		// CALLER DATA

		Group callerGroup = new Group(c, SWT.SHADOW_ETCHED_IN);
		callerGroup.setText(this.m_i18n.getString(getNamespace(),
				"callergroup", "label", this.m_language));

		callerGroup.setLayout(new GridLayout(2, false));
		callerGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		this.renderAsText(callerGroup, getAttribute(IJAMConst.ATTRIBUTE_NAME_FIRSTNAME), 210, 0, this.m_callerReadonly);
		this.renderAsText(callerGroup, getAttribute(IJAMConst.ATTRIBUTE_NAME_LASTNAME), 210, 0, this.m_callerReadonly);
		this.renderAsText(callerGroup, getAttribute(IJAMConst.ATTRIBUTE_NAME_ADDITIONAL), 0, 2, this.m_callerReadonly);
		this.renderAsText(callerGroup, getAttribute(IJAMConst.ATTRIBUTE_NAME_EMAIL), 0, 2, this.m_callerReadonly);
	
		// ADDRESS AREA

		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;

		Group addressGroup = new Group(callerGroup, SWT.SHADOW_ETCHED_IN);
		addressGroup.setText(this.m_i18n.getString(getNamespace(),
				"addressgroup", "label", this.m_language));

		addressGroup.setLayout(new GridLayout(3, false));
		addressGroup.setLayoutData(gd);
		
		this.renderAsText(addressGroup, getAttribute(IJAMConst.ATTRIBUTE_NAME_STREET), 370, 2, this.m_callerReadonly);
		this.renderAsText(addressGroup, getAttribute(IJAMConst.ATTRIBUTE_NAME_STREET_NO), 0, 0, this.m_callerReadonly);
		
		this.renderAsText(addressGroup, getAttribute(IJAMConst.ATTRIBUTE_NAME_POSTAL_CODE), 75, 0, this.m_callerReadonly);
		this.renderAsText(addressGroup, getAttribute(IJAMConst.ATTRIBUTE_NAME_CITY), 345, 2, this.m_callerReadonly);
		
		this.renderAsText(addressGroup, getAttribute(IJAMConst.ATTRIBUTE_NAME_COUNTRY), 0, 3, this.m_callerReadonly);
		

		Composite t = new Composite(c, SWT.NONE);
		t.setLayout(new GridLayout(2, false));

		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.verticalSpan = 2;
		gd.verticalIndent = 10;
		gd.verticalAlignment = GridData.VERTICAL_ALIGN_END;
		t.setLayoutData(gd);

		// IMAGE
		gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		if (OSUtils.isMacOSX()) {
			gd.heightHint = 110;
			final Label image = new Label(t, SWT.NONE);
			image.setLayoutData(gd);
			image.setImage(this.getCallerImage());

			if (hasCallerImage()) {
				image.setToolTipText(getCallerImagePath());
			} else {
				image.setToolTipText(m_i18n.getString(getNamespace(), "pixel", "label",
						m_language));
			}
			
			new Label(t, SWT.NONE);
			image.addMouseListener(new MouseListener() {

				public void mouseDoubleClick(MouseEvent arg0) {
					if (!m_callerReadonly) {
						FileDialog fde = new FileDialog(getShell(), SWT.OPEN);
						fde.setFilterExtensions(new String[] { "*.jpg", "*.jpeg",
								"*.gif", "*.png" });
						fde.setText(m_i18n.getString(getNamespace(), "select",
								"label", m_language));
						fde
								.setFilterPath(PathResolver.getInstance(getRuntime()).resolve(getAttributeValue(IJAMConst.ATTRIBUTE_NAME_IMAGEPATH)));

						String imagePath = fde.open();
						if (imagePath != null) {
							setAttributeValue(IJAMConst.ATTRIBUTE_NAME_IMAGEPATH,
									PathResolver.getInstance(getRuntime()).encode(imagePath));
							image.setImage(getNewImage(imagePath));
							if (hasCallerImage()) {
								image.setToolTipText(getCallerImagePath());
							}
							setPageComplete(isComplete());
						}
					}
				}

				public void mouseDown(MouseEvent arg0) {

				}

				public void mouseUp(MouseEvent arg0) {

				}});
			Menu m = new Menu(image);
			MenuItem mi = new MenuItem(m, SWT.PUSH);
			mi.setText(m_i18n.getString(getNamespace(), "remove", "label",
					m_language));
			mi.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					setAttributeValue(IJAMConst.ATTRIBUTE_NAME_IMAGEPATH, "");
					image.setImage(getNewImage(""));
					image.setToolTipText(m_i18n.getString(getNamespace(), "pixel", "label",
							m_language));
					
					// check if number image exist
					if (m_caller!=null && m_caller.getPhoneNumber().getTelephoneNumber().length()>0) {
						File photo = new File(PathResolver.getInstance().getPhotoDirectory(), m_caller.getPhoneNumber().getTelephoneNumber()+".jpg");
						if (photo.exists()) {
							photo.delete();
						}
						photo = new File(PathResolver.getInstance().getPhotoDirectory(), m_caller.getPhoneNumber().getTelephoneNumber()+".png");
						if (photo.exists()) {
							photo.delete();
						}
					}
					
					setPageComplete(isComplete());
				}
			});

			image.setMenu(m);
			final Button gravatar = new Button(t, SWT.CHECK);
			gravatar.setText(
		    	this.m_i18n.getString(this.getNamespace(), "gravatar", "label", this.m_language)	
		    );
			gravatar.setSelection(this.m_caller.getAttributes().contains(IJAMConst.ATTRIBUTE_NAME_GRAVATAR)); 
			this.m_useGRAVATAR = this.m_caller.getAttributes().contains(IJAMConst.ATTRIBUTE_NAME_GRAVATAR);
			
			gravatar.setEnabled(!m_callerReadonly);
			
			gravatar.addSelectionListener(
				new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {				
						m_useGRAVATAR = gravatar.getSelection();
//						if (!m_useGRAVATAR) {
//							setAttributeValue(IJAMConst.ATTRIBUTE_NAME_IMAGEPATH, "");
//							image.setImage(getNewImage(""));
//							image.setToolTipText(m_i18n.getString(getNamespace(), "pixel", "label",
//									m_language));
//						}
						if (m_useGRAVATAR && m_caller.getAttributes().contains(IJAMConst.ATTRIBUTE_NAME_EMAIL)) {
							String email = m_caller.getAttribute(IJAMConst.ATTRIBUTE_NAME_EMAIL).getValue();
							if (email != null && email.length()>0) {	
								File dir = new File(PathResolver.getInstance().getPhotoDirectory());
								if (!dir.exists()) dir.mkdirs();
								
								File img = new File(dir, m_caller.getPhoneNumber().getTelephoneNumber()+".jpg");
								try {
									email = StringUtils.toMD5Hex(email);
									String gravatar = "http://www.gravatar.com/avatar/"+email+".jpg?d=404&s=120";
									URL url = new URL(gravatar);
									URLConnection c = url.openConnection();
									
									StringBuffer agent = new StringBuffer();
									agent.append("jAnrufmonitor Update Manager ");
									agent.append(IJAMConst.VERSION_DISPLAY);
									
									c.setDoInput(true);
									c.setRequestProperty(
										"User-Agent",
										agent.toString());
									c.connect();

									Object o = url.openStream();
									if (o instanceof InputStream) {
										BufferedInputStream bin = new BufferedInputStream((InputStream) o);
										FileOutputStream fos = new FileOutputStream(img);
										Stream.copy(bin, fos, true);
										
										setAttributeValue(IJAMConst.ATTRIBUTE_NAME_IMAGEPATH, img.getAbsolutePath());
										image.setImage(getNewImage(img.getAbsolutePath()));
										if (hasCallerImage()) {
											image.setToolTipText(getCallerImagePath());
										}
									}
								} catch (FileNotFoundException ex) {
									m_logger.info("No GRAVATAR found: "+ex.getMessage());
								} catch (IOException ex) {
									m_logger.info("IOException: "+ex.getMessage());
								}
							}
						}
						setPageComplete(isComplete());
					}
				}	
		    );
		} else {
			final Button image = new Button(t, SWT.PUSH);
			image.setLayoutData(gd);
			image.setImage(this.getCallerImage());
		
			if (hasCallerImage()) {
				image.setToolTipText(getCallerImagePath());
			} else {
				image.setToolTipText(m_i18n.getString(getNamespace(), "pixel", "label",
						m_language));
			}
			
			new Label(t, SWT.NONE);
			image.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					if (!m_callerReadonly) {
						FileDialog fde = new FileDialog(getShell(), SWT.OPEN);
						fde.setFilterExtensions(new String[] { "*.jpg", "*.jpeg",
								"*.gif", "*.png" });
						fde.setText(m_i18n.getString(getNamespace(), "select",
								"label", m_language));
						fde
								.setFilterPath(PathResolver.getInstance(getRuntime()).resolve(getAttributeValue(IJAMConst.ATTRIBUTE_NAME_IMAGEPATH)));
		
						String imagePath = fde.open();
						if (imagePath != null) {
							setAttributeValue(IJAMConst.ATTRIBUTE_NAME_IMAGEPATH,
									PathResolver.getInstance(getRuntime()).encode(imagePath));
							image.setImage(getNewImage(imagePath));
							if (hasCallerImage()) {
								image.setToolTipText(getCallerImagePath());
							}
							setPageComplete(isComplete());
						}
					}
				}
			});
			Menu m = new Menu(image);
			MenuItem mi = new MenuItem(m, SWT.PUSH);
			mi.setText(m_i18n.getString(getNamespace(), "remove", "label",
					m_language));
			mi.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					setAttributeValue(IJAMConst.ATTRIBUTE_NAME_IMAGEPATH, "");
					image.setImage(getNewImage(""));
					image.setToolTipText(m_i18n.getString(getNamespace(), "pixel", "label",
							m_language));
					
					// check if number image exist
					if (m_caller!=null && m_caller.getPhoneNumber().getTelephoneNumber().length()>0) {
						File photo = new File(PathResolver.getInstance().getPhotoDirectory(), m_caller.getPhoneNumber().getTelephoneNumber()+".jpg");
						if (photo.exists()) {
							photo.delete();
						}
						photo = new File(PathResolver.getInstance().getPhotoDirectory(), m_caller.getPhoneNumber().getTelephoneNumber()+".png");
						if (photo.exists()) {
							photo.delete();
						}
					}
					setPageComplete(isComplete());
				}
			});

			image.setMenu(m);
			
			final Button gravatar = new Button(t, SWT.CHECK);
			gravatar.setText(
		    	this.m_i18n.getString(this.getNamespace(), "gravatar", "label", this.m_language)	
		    );
			gravatar.setSelection(this.m_caller.getAttributes().contains(IJAMConst.ATTRIBUTE_NAME_GRAVATAR)); 
			this.m_useGRAVATAR = this.m_caller.getAttributes().contains(IJAMConst.ATTRIBUTE_NAME_GRAVATAR);
			
			gravatar.setEnabled(!m_callerReadonly);
			
			gravatar.addSelectionListener(
				new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {				
						m_useGRAVATAR = gravatar.getSelection();
//						if (!m_useGRAVATAR) {
//							setAttributeValue(IJAMConst.ATTRIBUTE_NAME_IMAGEPATH, "");
//							image.setImage(getNewImage(""));
//							image.setToolTipText(m_i18n.getString(getNamespace(), "pixel", "label",
//									m_language));
//						}
						if (m_useGRAVATAR && m_caller.getAttributes().contains(IJAMConst.ATTRIBUTE_NAME_EMAIL)) {
							String email = m_caller.getAttribute(IJAMConst.ATTRIBUTE_NAME_EMAIL).getValue();
							if (email != null && email.length()>0) {	
								File dir = new File(PathResolver.getInstance().getPhotoDirectory());
								if (!dir.exists()) dir.mkdirs();
								
								File img = new File(dir, m_caller.getPhoneNumber().getTelephoneNumber()+".jpg");
								try {
									email = StringUtils.toMD5Hex(email);
									String gravatar = "http://www.gravatar.com/avatar/"+email+".jpg?d=404&s=120";
									URL url = new URL(gravatar);
									URLConnection c = url.openConnection();
									
									StringBuffer agent = new StringBuffer();
									agent.append("jAnrufmonitor Update Manager ");
									agent.append(IJAMConst.VERSION_DISPLAY);
									
									c.setDoInput(true);
									c.setRequestProperty(
										"User-Agent",
										agent.toString());
									c.connect();

									Object o = url.openStream();
									if (o instanceof InputStream) {
										BufferedInputStream bin = new BufferedInputStream((InputStream) o);
										FileOutputStream fos = new FileOutputStream(img);
										Stream.copy(bin, fos, true);
										
										setAttributeValue(IJAMConst.ATTRIBUTE_NAME_IMAGEPATH, img.getAbsolutePath());
										image.setImage(getNewImage(img.getAbsolutePath()));
										if (hasCallerImage()) {
											image.setToolTipText(getCallerImagePath());
										}
									}
								} catch (FileNotFoundException ex) {
									m_logger.info("No GRAVATAR found: "+ex.getMessage());
								} catch (IOException ex) {
									m_logger.info("IOException: "+ex.getMessage());
								}
							}
						}
						
						setPageComplete(isComplete());
					}
				}	
		    );
		}


		// NUMBER DATA

		tabFolder = new TabFolder(c, SWT.BORDER);
		gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 300;
		gd.heightHint = 75;
		gd.verticalAlignment = GridData.VERTICAL_ALIGN_BEGINNING;
		tabFolder.setLayoutData(gd);
		tabFolder.setFocus();

		List phones = m_caller.getPhonenumbers();

		for (int i = 0, j = phones.size(); i < j; i++) {
			new NumberView((IPhonenumber) phones.get(i), this.m_caller
					.getAttributes(), tabFolder, m_i18n.getString(
					getNamespace(), "std_phone", "label", m_language)).render();
		}
		
		tabFolder.pack();

		if (!this.m_callerReadonly && !m_caller.getPhoneNumber().isClired()) {
			// selectButton
			gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);

			Composite bc = new Composite(c, SWT.NONE);
			bc.setLayout(new GridLayout(2, false));

			final Button addPhone = new Button(bc, SWT.PUSH);

			addPhone.setLayoutData(gd);
			addPhone.setText(m_i18n.getString(getNamespace(), "add_phone",
					"label", m_language));

			addPhone.setVisible(!m_numberReadonly);

			final Button removePhone = new Button(bc, SWT.PUSH);

			removePhone.setLayoutData(gd);
			removePhone.setText(m_i18n.getString(getNamespace(),
					"remove_phone", "label", m_language));
			removePhone.setEnabled(false);
			removePhone.setVisible(!m_numberReadonly);

			addPhone.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					if (tabFolder.getItemCount() < 6) {
						new NumberView(getRuntime().getCallerFactory()
								.createPhonenumber(true), m_caller
								.getAttributes(), tabFolder, m_i18n.getString(
								getNamespace(), "std_phone", "label",
								m_language)).render();
						tabFolder.setSelection(tabFolder.getItemCount() - 1);
					} else
						addPhone.setEnabled(false);

					removePhone.setEnabled(true);

					setPageComplete(isComplete());
				}
			});

			removePhone.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					TabItem[] items = tabFolder.getSelection();
					if (items.length == 1 && tabFolder.getItemCount() > 1)
						items[0].dispose();

					addPhone.setEnabled(true);
					removePhone.setEnabled(tabFolder.getItemCount() > 1);
					setPageComplete(isComplete());
				}
			});

			if (tabFolder.getItemCount() > 1)
				removePhone.setEnabled(true);

			new Label(c, SWT.NONE);

			// CATEGORIES
			Group categoryGroup = new Group(c, SWT.SHADOW_ETCHED_IN);
			categoryGroup.setText(this.m_i18n.getString(getNamespace(),
					"categorygroup", "label", this.m_language));
			categoryGroup.setLayout(new GridLayout(2, true));
			categoryGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			categoryGroup.setToolTipText(m_i18n.getString(getNamespace(),
					"tooltipprefix", "label", m_language)
					+ IJAMConst.GLOBAL_VARIABLE_ATTRIBUTE_PREFIX
					+ IJAMConst.ATTRIBUTE_NAME_CATEGORY
					+ IJAMConst.GLOBAL_VARIABLE_ATTRIBUTE_POSTFIX);

			IAttribute category = this.m_caller
					.getAttribute(IJAMConst.ATTRIBUTE_NAME_CATEGORY);

			final Combo categoryCombo = new Combo(categoryGroup, SWT.READ_ONLY);

			String categories = getRuntime().getConfigManagerFactory()
					.getConfigManager().getProperty(Editor.NAMESPACE,
							"categories");
			String[] tmp = categories.split(",");
			String[] categoryList = new String[tmp.length + 1];
			categoryList[0] = "";
			for (int i = tmp.length-1, k=1; i >=0; i--, k++) {
				categoryList[k] = tmp[i];
			}
			int select = 0;
			for (int i = 0; i < categoryList.length; i++) {
				categoryCombo.setData(categoryList[i], categoryList[i]);
				if (category != null
						&& categoryList[i]
								.equalsIgnoreCase(category.getValue())) {
					select = i;
				}
			}
			categoryCombo.setItems(categoryList);
			categoryCombo.select(select);

			// Add the handler to update the name based on input
			categoryCombo.addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent event) {
					String scat = categoryCombo.getItem(categoryCombo
							.getSelectionIndex());
					m_caller.setAttribute(getRuntime().getCallerFactory()
							.createAttribute(IJAMConst.ATTRIBUTE_NAME_CATEGORY,
									scat));
					if (scat == null || scat.trim().length() == 0) {
						m_caller.getAttributes().remove(
								IJAMConst.ATTRIBUTE_NAME_CATEGORY);
					}

					setPageComplete(isComplete());
				}
			});
		}
		c.pack();

		setPageComplete(isComplete());
		setControl(c);
	}
	
//	private boolean isCentralImageStore() {
//		File cis = new File(getCentralImageStorePath());
//		File image = this.getCallerImageFile();
//		if (image!=null) {
//			return image.getAbsolutePath().startsWith(cis.getAbsolutePath());
//		}
//		return false;
//	}
	
//	private String getCentralImageStorePath() {
//		File cis = new File (PathResolver.getInstance(getRuntime()).getPhotoDirectory() + File.separator + "contacts");
//		if (!cis.exists()) {
//			cis.mkdirs();
//		}
//		return cis.getAbsolutePath();
//	}

	public boolean isComplete() {
		if (this.m_numberReadonly && this.m_callerReadonly)
			return false;

		if (this.getAttributeValue(IJAMConst.ATTRIBUTE_NAME_LASTNAME).trim()
				.length() == 0
				&& this.getAttributeValue(IJAMConst.ATTRIBUTE_NAME_FIRSTNAME).trim()
						.length() == 0) {
			setErrorMessage(this.m_i18n.getString(this.getNamespace(),
					"lastnameerror", "label", this.m_language));
			return false;
		}

		IPhonenumber pn = null;
		for (int i = 0, j = tabFolder.getItemCount(); i < j; i++) {
			pn = (IPhonenumber) ((NumberView) tabFolder.getItem(i).getData())
					.getPhoneNumber();
			if (!PhonenumberAnalyzer.getInstance(getRuntime()).isInternal(pn)) {
				if (!m_allowClirEdit && pn.getCallNumber().length() < 1) {
					setErrorMessage(this.m_i18n.getString(this.getNamespace(),
							"numbererror", "label", this.m_language));
					return false;
				}
			} else {
				if (!m_allowClirEdit && pn.getCallNumber().length() == 0) {
					setErrorMessage(this.m_i18n.getString(this.getNamespace(),
							"internalerror", "label", this.m_language));
					return false;
				}
			}
			if (pn.isClired()) {
				if (!m_allowClirEdit) {
					setErrorMessage(this.m_i18n.getString(this.getNamespace(),
							"numbererror", "label", this.m_language));
					return false;
				}
			}
		}

		return super.isComplete();
	}

	private String getAttributeValue(String attName) {
		if (this.m_caller.getAttributes().contains(attName)) {
			return this.m_caller.getAttribute(attName).getValue();
		}
		return "";
	}

	private void setAttributeValue(String attName, String value) {
		this.m_caller.setAttribute(this.getRuntime().getCallerFactory()
				.createAttribute(attName, value));
	}

	private boolean hasCallerImage() {
		if (this.m_caller != null) {
			return ImageHandler.getInstance().hasImage(this.m_caller);
		}
		return false;
	}
	
	private String getCallerImagePath() {
		if (this.m_caller != null) {
			return PathResolver.getInstance(getRuntime()).resolve(ImageHandler.getInstance().getImagePath(this.m_caller));
		}
		return "";
	}
	
	private Image getNewImage(String imagePath) {
		File img = new File(imagePath);
		if (img.exists()) {
			try {
				InputStream in = new FileInputStream(img);
				ImageData id = new ImageData(in);
	
				// calculate proportions
				float height = ((float) id.height / (float) id.width) * 90;
				id = id.scaledTo(90, (int) height);
	
				in.close();
				return new Image(DisplayManager.getDefaultDisplay(), id);
			} catch (FileNotFoundException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			} catch (IOException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			} catch (Exception e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			}
		}
		
		Image imgd = SWTImageManager.getInstance(this.getRuntime()).get(
				IJAMConst.IMAGE_KEY_EMPTYCALLER_JPG);
		imgd = new Image(DisplayManager.getDefaultDisplay(), imgd.getImageData()
				.scaledTo(90, 110));
		return imgd;
	}
	
	private Image getCallerImage() {
		InputStream in = null;

		if (this.m_caller != null) {
			in = ImageHandler.getInstance().getImageStream(this.m_caller);
			if (in!=null) {
				try {
					ImageData id = new ImageData(in);

					// calculate proportions
					float height = ((float) id.height / (float) id.width) * 90;
					id = id.scaledTo(90, (int) height);

					in.close();
					return new Image(DisplayManager.getDefaultDisplay(), id);
				} catch (FileNotFoundException e) {
					this.m_logger.log(Level.SEVERE, e.getMessage(), e);
				} catch (IOException e) {
					this.m_logger.log(Level.SEVERE, e.getMessage(), e);
				} catch (Exception e) {
					this.m_logger.log(Level.SEVERE, e.getMessage(), e);
				}
			} 
		}
		Image img = SWTImageManager.getInstance(this.getRuntime()).get(
				IJAMConst.IMAGE_KEY_EMPTYCALLER_JPG);
		img = new Image(DisplayManager.getDefaultDisplay(), img.getImageData()
				.scaledTo(90, 110));
		return img;
	}
	
	private void renderAsText(final Composite parent, final IAttribute attribute, int width, int span, boolean readonly) {
		final Text text = new Text(parent, SWT.BORDER);
		GridData gdi = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		if (width>0)
			gdi.widthHint = width;
		if (span>0)
			gdi.horizontalSpan = span;
		text.setLayoutData(gdi);
		
		text.setText((this.isEmpty(attribute) ? getEmptyText(attribute) : attribute.getValue()));
		text.setForeground((this.isEmpty(attribute) ? new Color(parent.getDisplay(), 190,190,190) : new Color(parent.getDisplay(), 0,0,0)));
		text.setToolTipText(this.m_i18n.getString(this.getNamespace(),
				"tooltipprefix", "label", this.m_language)
				+ IJAMConst.GLOBAL_VARIABLE_ATTRIBUTE_PREFIX
				+ attribute.getName()
				+ IJAMConst.GLOBAL_VARIABLE_ATTRIBUTE_POSTFIX);
		
		text.setEditable(!readonly);
	//	text.setFocus();
	
		text.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent event) {
				setAttributeValue(attribute.getName(), (text.getText().equalsIgnoreCase(getEmptyText(attribute))? "" : text
						.getText()));

				setPageComplete(isComplete());
			}
		});

		text.addFocusListener(new FocusAdapter() {
			public void focusGained(org.eclipse.swt.events.FocusEvent e) {
				if (text.getText().equalsIgnoreCase(getEmptyText(attribute))) {
					text.setText("");
				}
				text.setForeground(new Color(parent.getDisplay(), 0,0,0));
			}
			public void focusLost(org.eclipse.swt.events.FocusEvent e) {
				if (text.getText().length()==0) {
					text.setText(getEmptyText(attribute));
					text.setForeground(new Color(parent.getDisplay(), 190,190,190));
				}
			}
		});
	}
	
	private String getEmptyText(IAttribute attribute) {
		return this.m_i18n.getString(this.getNamespace(), attribute.getName(),
				"label", this.m_language);
	}
	
	private boolean isEmpty(IAttribute attribute) {
		return (attribute==null || attribute.getValue()==null || attribute.getValue().length()==0);
	}
	
	private IAttribute getAttribute(String attName) {
		if (this.m_caller.getAttributes().contains(attName)) {
			return this.m_caller.getAttribute(attName);
		}
		return getRuntime().getCallerFactory().createAttribute(attName, "");
	}
}
