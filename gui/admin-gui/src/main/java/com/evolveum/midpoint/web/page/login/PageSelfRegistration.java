package com.evolveum.midpoint.web.page.login;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEventSink;
import org.apache.wicket.extensions.markup.html.captcha.CaptchaImageResource;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.feedback.ContainerFeedbackMessageFilter;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.INamedParameters.NamedPair;
import org.apache.wicket.util.string.StringValue;
import org.apache.wicket.request.component.IRequestablePage;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.common.policy.StringPolicyUtils;
import com.evolveum.midpoint.common.policy.ValuePolicyGenerator;
import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteTextPanel;
import com.evolveum.midpoint.gui.api.component.captcha.CaptchaPanel;
import com.evolveum.midpoint.gui.api.component.password.PasswordPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LimitationsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NonceCredentialsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NonceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.StringPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import net.sf.jasperreports.components.map.ItemData;

//"http://localhost:8080/midpoint/confirm/registrationid=" + newUser.getOid()
//+ "/token=" + userType.getCostCenter() + "/roleId=00000000-0000-0000-0000-000000000008";
@PageDescriptor(url = "/registration")
public class PageSelfRegistration extends PageRegistrationBase {

	private static final Trace LOGGER = TraceManager.getTrace(PageSelfRegistration.class);

	private static final String DOT_CLASS = PageSelfRegistration.class.getName() + ".";

	private static final String ID_MAIN_FORM = "mainForm";
	private static final String ID_FIRST_NAME = "firstName";
	private static final String ID_LAST_NAME = "lastName";
	private static final String ID_EMAIL = "email";
	private static final String ID_PASSWORD = "password";
	private static final String ID_SUBMIT_REGISTRATION = "submitRegistration";
	private static final String ID_REGISTRATION_SUBMITED = "registrationInfo";
	private static final String ID_FEEDBACK = "feedback";
	private static final String ID_BACK = "back";

	private static final String ID_CAPTCHA = "captcha";
	
	private static final String OPERATION_SAVE_USER = DOT_CLASS + "saveUser";
	private static final String OPERATION_LOAD_USER = DOT_CLASS + "loadUser";

	private static final String PARAM_USER_OID = "user";
	
	private static final long serialVersionUID = 1L;

	private IModel<UserType> userModel;

	private boolean submited = false;
	String randomString = null;
	String captchaString = null;

	public PageSelfRegistration() {
		this(null);
	}

	public PageSelfRegistration(PageParameters pageParameters) {
		super();

		
		final String userOid = getOidFromParams(pageParameters);
		
		userModel = new LoadableModel<UserType>(false) {
			private static final long serialVersionUID = 1L;

			@Override
			protected UserType load() {
				return createUserModel(userOid);
			}
		};
		
		initLayout();

	}

	private String getOidFromParams(PageParameters pageParameters){
		if (pageParameters == null) {
			return null;
		}
		StringValue oidValue = pageParameters.get(PARAM_USER_OID);
		if (oidValue != null) {
			return oidValue.toString();
		}
		return null;
	}
	
	private UserType createUserModel(final String userOid) {
		
		if (userOid != null) {
			PrismObject<UserType> result = runPrivileged(new Producer<PrismObject<UserType>>() {
			
					@Override
					public PrismObject<UserType> run() {
						LOGGER.trace("Loading preregistered user with oid {}.", userOid);
						Task task = createAnonymousTask(OPERATION_LOAD_USER);
						OperationResult result = new OperationResult(OPERATION_LOAD_USER);
						PrismObject<UserType> user = WebModelServiceUtils.loadObject(UserType.class, userOid, PageSelfRegistration.this, task, result);
						result.computeStatus();
						return user;
					}
				
			});
			
			if (result == null) {
				LOGGER.error("Failed to load preregistered user");
				getSession().error(createStringResource("PageSelfRegistration.invalid.registration.link").getString());
				throw new RestartResponseException(PageLogin.class);
			}
			return result.asObjectable();
		}
		
		return instantiateUser();

	}
	
	private UserType instantiateUser(){
		PrismObjectDefinition<UserType> userDef = getUserDefinition();
		PrismObject<UserType> user;
		try {
			user = userDef.instantiate();
		} catch (SchemaException e) {
			UserType userType = new UserType();
			user = userType.asPrismObject();

		}
		return user.asObjectable();
	}
	
	private PrismObjectDefinition<UserType> getUserDefinition(){
		return getPrismContext().getSchemaRegistry()
				.findObjectDefinitionByCompileTimeClass(UserType.class);
	}

	private void initLayout() {
		
		Form<?> mainForm = new Form<>(ID_MAIN_FORM);
		initAccessBehaviour(mainForm);
		add(mainForm);
		
		//feedback
	    FeedbackPanel feedback = new FeedbackPanel(ID_FEEDBACK, new ContainerFeedbackMessageFilter(PageSelfRegistration.this));
        feedback.setOutputMarkupId(true);
        mainForm.add(feedback);

		TextPanel<String> firstName = new TextPanel<>(ID_FIRST_NAME,
				new PropertyModel<String>(userModel, UserType.F_GIVEN_NAME.getLocalPart() + ".orig") {

					private static final long serialVersionUID = 1L;

					@Override
					public void setObject(String object) {
						userModel.getObject().setGivenName(new PolyStringType(object));
					}
				});
		initInputProperties(feedback, firstName);
		mainForm.add(firstName);

		TextPanel<String> lastName = new TextPanel<>(ID_LAST_NAME,
				new PropertyModel<String>(userModel, UserType.F_FAMILY_NAME.getLocalPart() + ".orig") {

					private static final long serialVersionUID = 1L;

					@Override
					public void setObject(String object) {
						userModel.getObject().setFamilyName(new PolyStringType(object));
					}

				});
		initInputProperties(feedback, lastName);
		mainForm.add(lastName);

		TextPanel<String> email = new TextPanel<>(ID_EMAIL,
				new PropertyModel<String>(userModel, UserType.F_EMAIL_ADDRESS.getLocalPart()));
		initInputProperties(feedback, email);
		mainForm.add(email);

		ProtectedStringType initialPassword= null;
		PasswordPanel password = new PasswordPanel(ID_PASSWORD, Model.of(initialPassword));
		password.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
		password.getBaseFormComponent().setRequired(true);
		mainForm.add(password);

		CaptchaPanel captcha = new CaptchaPanel(ID_CAPTCHA);
		captcha.setOutputMarkupId(true);
		mainForm.add(captcha);

		AjaxSubmitButton register = new AjaxSubmitButton(ID_SUBMIT_REGISTRATION) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void onError(AjaxRequestTarget target,
					org.apache.wicket.markup.html.form.Form<?> form) {
				showErrors(target);
			}
			
			protected void onSubmit(AjaxRequestTarget target,
					org.apache.wicket.markup.html.form.Form<?> form) {

				submitRegistration(target);

			}

		};

		mainForm.add(register);

		MultiLineLabel label = new MultiLineLabel(ID_REGISTRATION_SUBMITED,
				createStringResource("PageSelfRegistration.registration.confirm.message"));
		add(label);
		label.add(new VisibleEnableBehaviour() {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return submited;
			}

			@Override
			public boolean isEnabled() {
				return submited;
			}

		});
		
		AjaxButton back = new AjaxButton(ID_BACK) {
			private static final long serialVersionUID = 1L;
			
			@Override
			public void onClick(AjaxRequestTarget target) {
				setResponsePage(PageLogin.class);
			}
		};
		mainForm.add(back);

	}
	
	private void initAccessBehaviour(Form mainForm) {
		mainForm.add(new VisibleEnableBehaviour() {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return !submited;
			}

			@Override
			public boolean isEnabled() {
				return !submited;
			}
		});
	}
	
	private void showErrors(AjaxRequestTarget target) {
		target.add(get(createComponentPath(ID_MAIN_FORM, ID_FEEDBACK)));
		target.add(getFeedbackPanel());
	}
	
	private void initInputProperties(FeedbackPanel feedback, TextPanel<String> input) {
		input.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
		input.getBaseFormComponent().setRequired(true);
		feedback.setFilter(new ContainerFeedbackMessageFilter(input.getBaseFormComponent()));
		
		input.add(new VisibleEnableBehaviour() {
			
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isEnabled() {
				return getOidFromParams(getPageParameters()) == null;
			}
			
		});
		
		
	}

	private CaptchaPanel getCaptcha() {
		return (CaptchaPanel) get(createComponentPath(ID_MAIN_FORM, ID_CAPTCHA));
	}

	private void submitRegistration(AjaxRequestTarget target) {

		if (!validateCaptcha(target)) {
			return;
		}
		
		OperationResult result = runPrivileged(new Producer<OperationResult>() {

			@Override
			public OperationResult run() {

				Task task = createAnonymousTask(OPERATION_SAVE_USER);
				task.setChannel(SchemaConstants.CHANNEL_GUI_SELF_REGISTRATION_URI);
				OperationResult result = new OperationResult(OPERATION_SAVE_USER);
				saveUser(task, result);
				result.computeStatus();
				return result;
			}

		});

		if (result.getStatus() == OperationResultStatus.SUCCESS) {
			submited = true;
			getSession()
					.success(createStringResource("PageSelfRegistration.registration.success").getString());
			
			switch (getSelfRegistrationConfiguration().getAuthenticationMethod()) {
				case MAIL:
					target.add(PageSelfRegistration.this);
					break;
				case SMS:
					throw new UnsupportedOperationException();
				case NONE:
					setResponsePage(PageLogin.class);
			}

		} else {
			getSession().error(
					createStringResource("PageSelfRegistration.registration.error", result.getMessage())
							.getString());
			removePassword();

		}

		updateCaptcha(target);
		target.add(getFeedbackPanel());
		target.add(this);

	}
	
	private boolean validateCaptcha(AjaxRequestTarget target) {
		CaptchaPanel captcha = getCaptcha();
		
		if (captcha.getRandomText() == null) {
			getSession().error(createStringResource("PageSelfRegistration.captcha.validation.failed").getString());
			target.add(getFeedbackPanel());
			removePassword();
			updateCaptcha(target);
			target.add(this);
			return false;
		}
		
		if (captcha.getCaptchaText() != null && captcha.getRandomText() != null) {
			if (!captcha.getCaptchaText().equals(captcha.getRandomText())) {
				getSession().error(createStringResource("PageSelfRegistration.captcha.validation.failed").getString());
				removePassword();
				updateCaptcha(target);
				target.add(getFeedbackPanel());
				target.add(this);
				return false;
			}
		}
		return true;
	}

	private void updateCaptcha(AjaxRequestTarget target) {

		CaptchaPanel captcha = (CaptchaPanel) get(createComponentPath(ID_MAIN_FORM, ID_CAPTCHA));
		captcha.invalidateCaptcha();
		Image image = (Image) get(createComponentPath(ID_MAIN_FORM, ID_CAPTCHA, "image"));
		target.add(image);
	}

	private void saveUser(Task task, OperationResult result) {
		
		ObjectDelta<UserType> userDelta = prepareUserDelta(task, result);
		userDelta.setPrismContext(getPrismContext());

		WebModelServiceUtils.save(userDelta, ModelExecuteOptions.createOverwrite(), result, task, PageSelfRegistration.this);
		result.computeStatus();

	}
	
	private ObjectDelta<UserType> prepareUserDelta(Task task, OperationResult result) {
		if (getOidFromParams(getPageParameters()) == null) {
			UserType userType = prepareUserToSave(task, result);
			return ObjectDelta.createAddDelta(userType.asPrismObject());
		} else {
			ObjectDelta<UserType> delta = ObjectDelta.createEmptyModifyDelta(UserType.class, getOidFromParams(getPageParameters()), getPrismContext());
			if (getSelfRegistrationConfiguration().getInitialLifecycleState() != null) {
				delta.addModificationReplaceProperty(UserType.F_LIFECYCLE_STATE, getSelfRegistrationConfiguration().getInitialLifecycleState());
			}
			delta.addModificationReplaceProperty(SchemaConstants.PATH_PASSWORD_VALUE, createPassword().getValue());
			delta.addModificationReplaceContainer(SchemaConstants.PATH_NONCE, createNonce(getSelfRegistrationConfiguration().getNoncePolicy(), task, result).asPrismContainerValue());
			return delta;
			
		} 
	}

	private UserType prepareUserToSave(Task task,
			OperationResult result) {
		
		SelfRegistrationDto selfRegistrationConfiguration = getSelfRegistrationConfiguration();
		UserType userType = userModel.getObject();
		UserType userToSave = userType.clone(); 
		if (selfRegistrationConfiguration.getRequiredLifecycleState() != null) {
			String userLifecycle = userToSave.getLifecycleState();
			if (!selfRegistrationConfiguration.getRequiredLifecycleState().equals(userLifecycle)){
				getSession().error(createStringResource("PageSelfRegistration.registration.failed.unsatisfied.registration.configuration").getString());
				throw new RestartResponseException(this);
			}
			
		}
		
		CredentialsType credentials = createCredentials(selfRegistrationConfiguration.getNoncePolicy(), task, result);
		userToSave.setCredentials(credentials);
		if (selfRegistrationConfiguration.getInitialLifecycleState() != null) {
			userToSave.setLifecycleState(selfRegistrationConfiguration.getInitialLifecycleState());
		}
		
		try {
			getPrismContext().adopt(userToSave);
		} catch (SchemaException e) {
			// nothing to do, try without it
		}

		return userToSave;

	}
	
	private CredentialsType createCredentials(NonceCredentialsPolicyType noncePolicy, Task task, OperationResult result) {
		NonceType nonceType = createNonce(noncePolicy, task, result);
		
		PasswordType password = createPassword();
		CredentialsType credentials = new CredentialsType();
		credentials.setNonce(nonceType);
		credentials.setPassword(password);
		return credentials;

	}
	
	private NonceType createNonce(NonceCredentialsPolicyType noncePolicy, Task task, OperationResult result) {
		ProtectedStringType nonceCredentials = new ProtectedStringType();
		nonceCredentials.setClearValue(generateNonce(noncePolicy, task, result));

		NonceType nonceType = new NonceType();
		nonceType.setValue(nonceCredentials);
		return nonceType;
	}
	
	private PasswordType createPassword() {
		PasswordType password = new PasswordType();
		ProtectedStringType protectedString = new ProtectedStringType();
		protectedString.setClearValue(getPassword());
		password.setValue(protectedString);
		return password;
	}

	private String generateNonce(NonceCredentialsPolicyType noncePolicy, Task task, OperationResult result) {
		ValuePolicyType policy = null;
		
		if (noncePolicy != null && noncePolicy.getValuePolicyRef() != null) {
			PrismObject<ValuePolicyType> valuePolicy = WebModelServiceUtils.loadObject(ValuePolicyType.class,
					noncePolicy.getValuePolicyRef().getOid(), PageSelfRegistration.this, task, result);
			policy = valuePolicy.asObjectable();
		}

		return ValuePolicyGenerator.generate(policy != null ? policy.getStringPolicy() : null, 24, result);
	}

	private String getPassword() {
		PasswordPanel password = (PasswordPanel) get(
				createComponentPath(ID_MAIN_FORM, ID_PASSWORD));
		return (String) password.getBaseFormComponent().getModel().getObject();
	}
	
	private void removePassword() {
		PasswordPanel password = (PasswordPanel) get(
				createComponentPath(ID_MAIN_FORM, ID_PASSWORD));
		password.getBaseFormComponent().getModel().setObject(null);
	}
	
	@Override
    protected void createBreadcrumb() {
        //don't create breadcrumb for registration page
    }


}