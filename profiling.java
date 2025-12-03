package ec.com.sidesoft.happypay.web.services.service;

import org.apache.ddlutils.model.UtilsCompare;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse; 

import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Category;
import org.openbravo.model.common.businesspartner.Location;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.service.web.WebService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Locale;

import ec.com.sidesoft.actuaria.special.customization.Scactu_Log;
import ec.com.sidesoft.credit.factory.SscfCreditOperation;
import ec.com.sidesoft.credit.factory.maintenance.CivilStatus;
import ec.com.sidesoft.credit.factory.maintenance.Profession;
import ec.com.sidesoft.credit.simulator.scsl_Product;
import ec.com.sidesoft.customer.exception.Ecsce_CustomerException;
import ec.com.sidesoft.happypay.web.services.shppws_detailDue;
import ec.com.sidesoft.happypay.web.services.monitor.BlacklistsEntry;
import ec.com.sidesoft.happypay.web.services.monitor.MonitorManager;
import ec.com.sidesoft.happypay.customizations.shpctBinnacle;
import ec.com.sidesoft.happypay.pev.shppev_age;
import ec.com.sidesoft.happypay.pev.shppev_emailReason;
import ec.com.sidesoft.happypay.pev.shppev_internalDebt;
import ec.com.sidesoft.happypay.pev.shppev_lifestyleReason;
import ec.com.sidesoft.happypay.pev.shppev_phoneReason;
import ec.com.sidesoft.happypay.pev.shppev_rIdentification;
import ec.com.sidesoft.happypay.pev.shppev_reasonProfession;
import ec.com.sidesoft.happypay.pev.credit.InstexceptionEqfx;
import ec.com.sidesoft.happypay.pev.credit.Shppec_CredCurr;
import ec.com.sidesoft.happypay.pev.credit.Shppec_CredExp;
import ec.com.sidesoft.happypay.pev.credit.Shppec_CredPen;
import ec.com.sidesoft.happypay.pev.credit.Shppec_ExpActual;
import ec.com.sidesoft.happypay.pev.credit.Shppec_Lawsuit;
import ec.com.sidesoft.happypay.pev.credit.Shppec_ParallelC;
import ec.com.sidesoft.happypay.pev.credit.shppec_portpen;
import ec.com.sidesoft.happypay.pev.evaluation.shppee_NewCustomerScore;
import ec.com.sidesoft.happypay.pev.evaluation.shppee_Quotas;
import ec.com.sidesoft.happypay.pev.evaluation.shppee_RiskIndex;
import ec.com.sidesoft.happypay.pev.reference.ShpperReferenceMatrix;
import ec.com.sidesoft.happypay.web.services.shppwsPartnertype;
import ec.com.sidesoft.happypay.web.services.shppws_config;
import ec.com.sidesoft.ws.equifax.SweqxEquifax;
import ec.sidesoft.happypay.opportunity.shpop_orderline;
import it.openia.crm.Opcrmopportunities;

import java.util.Base64;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertPathValidatorException.Reason;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.joda.time.DateTime;
import org.joda.time.Days;

import org.openbravo.base.provider.OBProvider;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.client.kernel.RequestContext;

public class profiling implements WebService{
	String typeClientSynergy = "";
	boolean statusEquifax = true;
	
    // Variables para token equifax
    private String cachedToken = null;
    private long tokenExpiryTime = 0;
    private static final Logger log4j = Logger.getLogger(profiling.class.getName());


	private static final long serialVersionUID = 1L;
	public static Date truncateDate(Date date) {
	    Calendar cal = Calendar.getInstance();
	    cal.setTime(date);
	    cal.set(Calendar.HOUR_OF_DAY, 0);
	    cal.set(Calendar.MINUTE, 0);
	    cal.set(Calendar.SECOND, 0);
	    cal.set(Calendar.MILLISECOND, 0);
	    return cal.getTime(); 
	}
	
	public void doGet(String path, HttpServletRequest request, HttpServletResponse response) throws Exception {
		String Error = "";
		boolean statusSinergia = true;
		////////////////////////////////////AQUI EQUIFAX ESTADO
		JSONObject jsonMonitor = new JSONObject();
		jsonMonitor.put("SHPPWS_SideSoft_Profiling", "Service"+0);
		jsonMonitor.put("startSHPPWS_SideSoft_Profiling", LocalDateTime.now());
		jsonMonitor.put("typeSHPPWS_SideSoft_Profiling", "Perfilamiento");
		
		OBCriteria<shppws_config> queryApi= OBDal.getInstance().createCriteria(shppws_config.class);
	    shppws_config accesApi = (shppws_config) queryApi.uniqueResult();
	    log_records logger = new log_records();
        String requestParameters =  request.getQueryString();
	    Scactu_Log log = logger.log_start_register(accesApi, "profiling", requestParameters);
	    
		//input variables
		String Interface = request.getParameter("Interface");
		String Chanel = request.getParameter("Chanel");
		String Code_Commerce = request.getParameter("Code_Commerce");
		String Code_Agency = request.getParameter("Code_Agency");
		String Store_group = request.getParameter("Store_group");
		String Code_Product = request.getParameter("Code_Product");
		String ID = request.getParameter("ID");
		String CellPhone = request.getParameter("CellPhone");
		String email = request.getParameter("email");
		String Amount = request.getParameter("Amount");
		String Entrance = request.getParameter("Entrance");
		//String User = request.getParameter("User");
		String City_store_group = request.getParameter("City_store_group");
		String Province_store_group = request.getParameter("Province_store_group");
		
		// Blacklist - Entry
		BlacklistsEntry BlackList = new BlacklistsEntry();
		/*if (accesApi != null) {
			new Thread(() -> {*/
				try {
					BlackList.BlacklistEntryApi(accesApi, ID != null ? ID : "", CellPhone != null ? CellPhone : "");
				} catch (Exception e) {
					e.printStackTrace();
				}
			/*}).start();
		}*/
		
		/////////Filters Variables//////////
		boolean CustomerExeption_result=true;
        Date fechaActual = new Date();
		String filter="";
		
		String validatorRC = "";
		String filterRC = "-"; //Registro Civil
			String RCnames="";
			String RClastName1="";
			String RClastName2="";
			String RCname1="";
			String RCname2="";
			String RCgender="";
			String RCbirthdate="";
			String RCprofession="";
			String RCcivilStatus="";
			String RCnationality="";
			String RCnationalityAux="";
			
		String filterEQ = ""; //Equifax
			Double CV = new Double(0);
		    Double CC = new Double(0);
		    Double DJ = new Double(0);
		    String segment1 = "";
		    String segment2 = "";
		    String segment3 = "";
		    String newClient_segment="";
		    String newClient_scoreInclusion="";
		    Double Quota_Available =new Double(0);
		
		  //////////////////////////////////
		 //////////PRIMER FILTRO RC////////
		//////////////////////////////////
		OBCriteria<shppev_age> query = OBDal.getInstance().createCriteria(shppev_age.class);
	    List<shppev_age> reasonAges = query.list();
		int size = reasonAges.size();
		 Map<String, Object> persona = new HashMap<>();
		 persona.put("CellPhone", CellPhone);
		 persona.put("email", email);
		 persona.put("interface", Interface);
		 persona.put("channel", Chanel);
		 persona.put("commercialcode", Code_Commerce);
		 persona.put("agencycode", Code_Agency);
		 persona.put("shopgroup", Store_group);
		 persona.put("productcode", Code_Product);
		 persona.put("Amount", Amount);
		 persona.put("Identifier", ID);
		 persona.put("City_store_group", City_store_group);
		 persona.put("Province_store_group", Province_store_group); 

		    /*VALIDACION SOLO PARA CI DEL TERCERO*/
		 // Fecha actual
		 Date now = truncateDate(new Date());		    
		    OBCriteria<Ecsce_CustomerException> queryApi_exception = OBDal.getInstance().createCriteria(Ecsce_CustomerException.class);
		    queryApi_exception.add(Restrictions.and(
		    Restrictions.and(
		    		Restrictions.eq(Ecsce_CustomerException.PROPERTY_TAXID, persona.get("Identifier")),
		    		Restrictions.eq(Ecsce_CustomerException.PROPERTY_STOREGROUP, persona.get("shopgroup"))
		    		    	),
		    Restrictions.and(
		    		Restrictions.le(Ecsce_CustomerException.PROPERTY_STARTINGDATE, now),
		    		Restrictions.ge(Ecsce_CustomerException.PROPERTY_DATEUNTIL, now)
		    		    )       // dateUntil >= hoy
		    ));
		    queryApi_exception.addOrder(Order.desc(Ecsce_CustomerException.PROPERTY_CREATIONDATE)); // más reciente
		    queryApi_exception.setMaxResults(1); // solo uno
		    Ecsce_CustomerException accesApi_exception = (Ecsce_CustomerException) queryApi_exception.uniqueResult();
		    log_records logger_exeption = new log_records();

		    
		    OBCriteria<Ecsce_CustomerException> customerexception = OBDal.getInstance().createCriteria(Ecsce_CustomerException.class);
		    customerexception.add(Restrictions.and(
		    Restrictions.and(
		    		Restrictions.eq(Ecsce_CustomerException.PROPERTY_TAXID, persona.get("Identifier")),
		    		Restrictions.eq(Ecsce_CustomerException.PROPERTY_STOREGROUP, persona.get("shopgroup"))
				    		),
		    Restrictions.and(
		    		Restrictions.le(Ecsce_CustomerException.PROPERTY_STARTINGDATE, now),
		    		Restrictions.ge(Ecsce_CustomerException.PROPERTY_DATEUNTIL, now)
		    				)       // dateUntil >= hoy
				    ));
		    customerexception.addOrder(Order.desc(Ecsce_CustomerException.PROPERTY_CREATIONDATE)); // más reciente
		    customerexception.setMaxResults(1); // solo uno
		    
		    Ecsce_CustomerException cf = (Ecsce_CustomerException) customerexception.uniqueResult();
		    List<Ecsce_CustomerException> listcustomer = customerexception.list();

		    boolean CheckSinergia=validateSinergy();
	
	    if (size > 0) {
	    	try {
	        String apiResponse = getApiResponse(accesApi, ID, 1, jsonMonitor);
	        procesarApiRC(apiResponse, reasonAges, Store_group, persona);  //Matriz Edad
		         RCnames = (String) persona.get("RCnames");
		         RClastName1 = (String) persona.get("RClastName1");
		         RClastName2 = (String) persona.get("RClastName2");
		         RCname1 = (String) persona.get("RCname1");
		         RCname2 = (String) persona.get("RCname2");
		         RCgender = (String) persona.get("RCgender");
		         RCbirthdate = (String) persona.get("RCbirthdate");
		         RCprofession = (String) persona.get("RCprofession");
		         RCcivilStatus = (String) persona.get("RCcivilStatus");
		         RCnationality = (String) persona.get("RCnationality");
		         RCnationalityAux = (String) persona.get("RCnationalityAux");
		         filterRC = (String)persona.get("filterRC"); //resultado segmento Matriz Edad
		         filter=filterRC;
		         validatorRC = (String) persona.get("validatorRC") != null ? (String) persona.get("validatorRC"):"";
			     Error = (String) persona.get("msgLN") != null ? (String) persona.get("msgLN"): "";
	    	}catch(Exception e) {
	    		filter = "R";
		    	persona.put("msgLN", "Servicio de Recover fuera de línea");
		    	persona.put("idLN",ID);
		    	persona.put("matrizReason","Recover");
		    	Error = e.getMessage();
	    	}
	    }
	    
	    BusinessPartner partner = null;
	    if(!validatorRC.equals("R")) {
		    	try {
		    		if(customerexception.list().size()>0) {
			    		if(!cf.getStartingDate().equals(null)&& !cf.getDateuntil().equals(null)) {
			    		    Date fechaInicio = cf.getStartingDate();
			    		    Date fechaFin = cf.getDateuntil();
			    		    Days dinicio = Days.daysBetween(new DateTime(fechaInicio.getTime()), new DateTime( fechaActual.getTime()));
			    		    Days dfin = Days.daysBetween(new DateTime(fechaFin.getTime()), new DateTime( fechaActual.getTime()));
			                 int daysInicio = dinicio.getDays();
			                 int daysFin = dfin.getDays();
				    		if(customerexception.list().size()>0 && cf.isActive() && daysInicio>=0 && daysFin<=0) {
				    				
				    			CustomerExeption_result=false;
				    		}else {
				    			CustomerExeption_result=true;
				    		}
			    		}
		    		}else {
		    			CustomerExeption_result=true;
		    		}
			    	partner = validatePartner( accesApi,  ID, persona );
			    }catch(Exception e) {
			    	filter = "R";
					persona.put("msgLN", e.getMessage() + "");
					persona.put("idLN", ID);
					persona.put("matrizReason", "Recover");
					Error = e.getMessage();
			    }
	    }
	    
	    
		  //////////////////////////////////
		 /////// FILTRO Listas Negras /////
		//////////////////////////////////
    	if(filter.equals("C") && CustomerExeption_result) {
    		try {
    			Boolean validateServices=true;
    			String responseLN_CI="";
    			Boolean validate_CI = false;
    			String responseLN_Celphone="";
    			Boolean validate_Celphone=false;
    			String responseLN_Email ="";
    			Boolean validate_Email = false;
    			try { 
        			if(!validateBlackListcheckCedula()) {
	    				responseLN_CI = getApiResponse(accesApi, ID, 2, jsonMonitor);
	    				validate_CI =  validateLN_CI(ID, responseLN_CI, persona);
        			}else {
        				validate_CI=true;
        			}
    			}catch (Exception e) {
    				filter = accesApi.getLN1Response();
    		    	persona.put("msgLN", e.getMessage()+"");
    		    	persona.put("idLN",ID);
    		    	persona.put("matrizReason","Motivo Cédulas");
    		    	validate_CI = (filter != null && filter.equals("C")) ? true : false;
    		    	Error = e.getMessage();
    			};
    			
    			if(filter.equals("R")){validateServices = false;}
				if (validateServices) {
					try {
						if (validate_CI) {
		        			if(!validateBlackListchecktelefono()) {
								responseLN_Celphone = getApiResponse(accesApi, CellPhone, 3, jsonMonitor);
								validate_Celphone = validateLN_Cellphone(CellPhone, responseLN_Celphone, persona);
		        			}else {
		        				validate_Celphone=true;
		        			}
						}else {filter="R";}
					} catch (Exception e) {
						filter = accesApi.getLN2Response();
						persona.put("msgLN", e.getMessage() + "");
						persona.put("idLN", ID);
						persona.put("matrizReason", "Motivo Teléfonos");
						validate_Celphone = (filter != null && filter.equals("C")) ? true : false;
						Error = e.getMessage();
					}
				}
    			
    			
    			if(filter.equals("R")){validateServices = false;}
				if (validateServices) {
					try {
						if (validate_Celphone) {
		        			if(!validateBlackListcheckemail()) {
								responseLN_Email = getApiResponse(accesApi, email, 4, jsonMonitor);
								validate_Email = validateLN_Email(email, responseLN_Email, persona);
		        			}else {
		        				validate_Email=true;
		        			}
						}else {filter="R";}
					} catch (Exception e) {
						filter = accesApi.getLN3Response();
						persona.put("msgLN", e.getMessage() + "");
						persona.put("idLN", ID);
						persona.put("matrizReason", "Motivo Correos");
						validate_Email = (filter != null && filter.equals("C")) ? true : false;
						Error = e.getMessage();
					}
				}
    			
    			
    			if(filter.equals("R")){validateServices = false;}
				if (validateServices) {
					try {
						if (validate_Email) {
							Boolean validate_Profession = validateLN_Profession(persona);
							if (validate_Profession) {
								filter = "C";
							} else {
								filter = "R";
							}
						}else {filter="R";}
					} catch (Exception e) {
						filter = "R";
						persona.put("msgLN", e.getMessage() + "");
						persona.put("idLN", ID);
						persona.put("matrizReason", "Motivo Profesión");
						Error = e.getMessage();
					}
				}
    			
    		}catch(Exception e) {
    			filter = "R";
		    	persona.put("msgLN", "Servicio de Listas Negras fuera de línea");
		    	persona.put("idLN",ID);
		    	persona.put("matrizReason","Servicio de Listas Negras");
		    	Error = e.getMessage();
    		}
    	}
	    
    	  //////////////////////////////////
		 //// FILTRO Deudas Internas //////
		//////////////////////////////////
    	if(filter.equals("C")&& CustomerExeption_result) {
    		try {
	    	String response_DI = getApiResponse(accesApi, ID, 5, jsonMonitor);
	    	Boolean validate_DI =  validateDataDI(response_DI, persona);
		    	if(validate_DI) {
		    		filter="C";
		    	}else {
		    		filter="R";
		    	}
    		}catch(Exception e) {
		    	persona.put("msgLN", e.getMessage()+"");
		    	persona.put("idLN",ID);
		    	persona.put("matrizReason","Servicio de Deudas Internas");
		    	filter = accesApi.getLndiResponse();
		    	Error = e.getMessage();
    		}
    	}
    	
    	  //////////////////////////////////
		 // FILTRO Cliente nuevo antiguo //
		//////////////////////////////////
    	Boolean clientnew=false;
    	String response_DI=null;
    	//verifica que el check de sinergia este activo y el estado de sinergia sea diferente de R, si da R se pasara a false
    	if(CheckSinergia && statusSinergia) {
    		response_DI = getApiResponse(accesApi, ID, 8, jsonMonitor);	   
			if (response_DI.equals("Error sinergia")) {
				filter = "R";
				persona.put("msgLN", "Servicio de Deudas Internas fuera de línea");
				persona.put("idLN", ID);
				persona.put("matrizReason", "Servicio de Deudas Internas");
				Error = "Servicio de Deudas Internas fuera de línea";
				statusSinergia = false;
    		}
		   	if(filter.equals("C")&& CustomerExeption_result) {
		   	typeClientSynergy = classificationClient(response_DI);
		    String validateCustomer_old_new = validateStatusPartner(response_DI);
			    	if(validateCustomer_old_new.equals("CLIENTE NUEVO")) {//customer new
			    		filter="C";
			    		clientnew = true;
			    	}else {//customer old
			    		clientnew = false;
			    		if(validateCustomer_old_new.equals("CLIENTE APROBADO")) {
			    			filter="C";
			    		//}else {
			    		Boolean validateCustomer_old_cc =  customerOldCC(partner, persona,validateCustomer_old_new);//Matriz credito castigado
			    		if(validateCustomer_old_cc) {
			    			Boolean validateCustomer_old_cv = customerOldCV(partner, persona,validateCustomer_old_new);//Matriz credito vigente
			    			if(validateCustomer_old_cv) {
			    				Boolean validateCustomer_old_cvv = customerOldCVV(partner, persona,validateCustomer_old_new);//Matriz credito vigente vencido
				    			if(validateCustomer_old_cvv) {
				    				filter="C";
				    			}else {
				    				filter="R";
				    				statusSinergia=false;
				    			}
			    			}else {
			    				filter="R";
			    				statusSinergia=false;
			    			}
			    		}else {
			    			filter="R";
		    				statusSinergia=false;
			    		}
				    	}else {
				    		filter="R";
		    				statusSinergia=false;
				    		Boolean validateCustomer_old_cc =  customerOldCC(partner, persona,validateCustomer_old_new);//Matriz credito castigado
				    		if(validateCustomer_old_cc) {
				    			Boolean validateCustomer_old_cv = customerOldCV(partner, persona,validateCustomer_old_new);//Matriz credito vigente
				    			if(validateCustomer_old_cv) {
				    				Boolean validateCustomer_old_cvv = customerOldCVV(partner, persona,validateCustomer_old_new);//Matriz credito vigente vencido
					    			if(validateCustomer_old_cvv) {
					    				filter="C";
					    			}else {
					    				filter="R";
					    				statusSinergia=false;
					    			}
				    			}else {
				    				filter="R";
				    				statusSinergia=false;
				    			}
				    		}else {
				    			filter="R";
			    				statusSinergia=false;
				    		}
				    	}
			    	}			    	
		   	}
	    //verifica que el check de sinergia este activo y el estado de sinergia sea IGUAL a R, si da R se pasara a false
    	}if(CheckSinergia && !statusSinergia) {
    		filter="C";
    		if(filter.equals("C")&& CustomerExeption_result) {
        		typeClientSynergy = "Error en sinergia";
    		   	Boolean validateCustomer_old_new =  customerNewOld(persona, partner);
    			    	if(validateCustomer_old_new) {//customer new
    			    		filter="C";
    			    		clientnew = true;
    			    	}else {//customer old
    			    		clientnew = false;
    			    		Boolean validateCustomer_old_cc =  customerOldCC_uncheck(partner, persona);//Matriz credito castigado
    			    		if(validateCustomer_old_cc) {
    			    			Boolean validateCustomer_old_cv = customerOldCV_uncheck(partner, persona);//Matriz credito vigente
    			    			if(validateCustomer_old_cv) {
    			    				Boolean validateCustomer_old_cvv = customerOldCVV_uncheck(partner, persona);//Matriz credito vigente vencido
    				    			if(validateCustomer_old_cvv) {
    				    				filter="C";
    				    			}else {
    				    				filter="R";
    				    			}
    			    			}else {
    			    				filter="R";
    			    			}
    			    		}else {
    			    			filter="R";
    			    		}
    			    	}
    		   	}
    	//proceso normal cuando el check de sinergia no sea activo
    	} if(!CheckSinergia) {
    		if(filter.equals("C")&& CustomerExeption_result) {
    		   	Boolean validateCustomer_old_new =  customerNewOld(persona, partner);
    			    	if(validateCustomer_old_new) {//customer new
    			    		filter="C";
    			    		clientnew = true;
    			    	}else {//customer old
    			    		clientnew = false;
    			    		Boolean validateCustomer_old_cc =  customerOldCC_uncheck(partner, persona);//Matriz credito castigado
    			    		if(validateCustomer_old_cc) {
    			    			Boolean validateCustomer_old_cv = customerOldCV_uncheck(partner, persona);//Matriz credito vigente
    			    			if(validateCustomer_old_cv) {
    			    				Boolean validateCustomer_old_cvv = customerOldCVV_uncheck(partner, persona);//Matriz credito vigente vencido
    				    			if(validateCustomer_old_cvv) {
    				    				filter="C";
    				    			}else {
    				    				filter="R";
    				    			}
    			    			}else {
    			    				filter="R";
    			    			}
    			    		}else {
    			    			filter="R";
    			    		}
    			    	}
    		   	}
    	}
    	
		  //////////////////////////////////
		 //////////FILTRO Equifax//////////
		//////////////////////////////////
    	 if(filter.equals("R")) {
    	 	    persona.put("Exception_Institucion","false");
 	    }
		    if (filter.equals("C")&& CustomerExeption_result) {
		    	String validateCustomer=null;
		    	//verifica que el check de sinergia este activo y el estado de sinergia sea diferente de R, si da R se pasara a false
		    	if(CheckSinergia && statusSinergia) {
				    validateCustomer = validateStatusPartner(response_DI);
			        //-------------------------------------------------------------------------------
			    	if(validateCustomer.equals("CLIENTE APROBADO")) {
				    	Error = procesarApiEQ(partner, accesApi, ID, persona, jsonMonitor); //Aqui obtengo datos de Equifax y guardo en persona
				    	if(persona.get("Exception_Institucion").equals("true")) {
				    		filter=(String) persona.get("Segment");
				    	}else {
				    		filter="SP";
				    	}
			    		
			    	}else {
				    	Error = procesarApiEQ(partner, accesApi, ID, persona, jsonMonitor); //Aqui obtengo datos de Equifax y guardo en persona
					    String serviceValidate=(String)persona.get("ApiResponse");
					    if(Error != null && !(Error.equals(""))) {
					    	filter = "R";
		    				statusSinergia=false;
					    	persona.put("msgLN", Error);
					    	persona.put("idLN",ID);
					    	persona.put("matrizReason","Servicio Equifax");
					    }else {
					    	if(clientnew) {//proceso para Clientes nuevos
					    		if(persona.get("Exception_Institucion").equals("true")) {
					    			filterEQ=(String) persona.get("Segment");
						    		}else {
					    		CV = (Double) persona.get("CV");
								CC = (Double) persona.get("CC");
								DJ = (Double) persona.get("DJ");
							    Double auxValuesSummary= CV+CC+DJ;
							    	if (auxValuesSummary > 0) {//Si-> Compara en las tres matrices y devuleve un unico segmento
							    		validateEquifaxArrays(ID, Store_group, persona); //Valida deudad 3 matrices
							    		segment1=(String) persona.get("segment1ExpiredValues");
							    		segment2=(String) persona.get("segment2cartValues");
							    		segment3=(String) persona.get("segment3lawsuitValues");
							    		if(segment1.equals("R")||segment2.equals("R")||segment3.equals("R")) {
							    			filterEQ="R";
						    				statusSinergia=false;
							    			persona.put("message","Filtro Equifax, Cliente rechazado.");
								      }else {
								    	  ///////////////////////////////////////////////////////
								    	  if(!statusEquifax) {////////////////////////////////////AQUI EQUIFAX ESTADO
								    	  		filterEQ=getScoreNewClientDefault();
								    	  }else {
								    	  	filterEQ="SP";
									    	validateEquifaxScoreNewClientSP(persona,filterEQ);
							    			filterEQ=(String) persona.get("EQ_scoreClient");
								    	  }
								      }
							    	}else{ //NO-> Compara en la Matriz nuevo Cliente y devuleve un unico segmento
							    		try {
							    			if(!statusEquifax) {////////////////////////////////////AQUI EQUIFAX ESTADO
							    				filterEQ=getScoreNewClientDefault();
							    			}else {
								    			validateEquifaxScoreNewClient(persona);
//								    			if(Error.equals("")) {
//													OBCriteria<shppee_NewCustomerScore> defaultEQX = OBDal.getInstance().createCriteria(shppee_NewCustomerScore.class);
//													defaultEQX.add(Restrictions.eq(shppee_NewCustomerScore.PROPERTY_SHPPWSDEFAULTEQXSEG, true));
//													shppee_NewCustomerScore defaultEQ = (shppee_NewCustomerScore) defaultEQX.uniqueResult();
//													List<shppee_NewCustomerScore> resultList = defaultEQX.list();
//													if(resultList.size()>0) {
//														persona.put("EQ_scoreClient", defaultEQ.getENDSegment());
//													}
//													
//										      }
								    			filterEQ=(String) persona.get("EQ_scoreClient");
								    			if(filterEQ.equals("")) {
								    				filterEQ="R";
								    				statusSinergia=false;
								    			}
							    			}
							    		}catch (Exception e) {}
							    	}
						    		}
						    	}else {//proceso para clientes antiguos
							    	if(!statusEquifax) {////////////////////////////////////AQUI EQUIFAX ESTADO
							    		filterEQ = getDefaultRiskIndiex();
							    	}else {
								    	String parallelCreditMatrix=parallelCredits(partner, persona); //Créditos paralelos
								    	filterEQ = customerRiskIndex(partner, parallelCreditMatrix, persona); //Indice de Riesgo
							    	}
						    	}
						    filter=filterEQ;
					    }
			    	}
				//verifica que el check de sinergia este activo y el estado de sinergia sea IGUAL a R, si da R se pasara a false
		    	}else if(CheckSinergia && !statusSinergia) {
		    		Error = procesarApiEQ(partner, accesApi, ID, persona, jsonMonitor); //Aqui obtengo datos de Equifax y guardo en persona
				    String serviceValidate=(String)persona.get("ApiResponse");
				    
				    if(Error != null && !(Error.equals(""))) {
				    	filter = "R";
				    	persona.put("msgLN", Error);
				    	persona.put("idLN",ID);
				    	persona.put("matrizReason","Servicio Equifax");
				    }else {
				    	if(clientnew) {//proceso para Clientes nuevos
				    		if(persona.get("Exception_Institucion").equals("true")) {
				    			filterEQ=(String) persona.get("Segment");
					    		}else {
								CV = (Double) persona.get("CV");
								CC = (Double) persona.get("CC");
								DJ = (Double) persona.get("DJ");
							     Double auxValuesSummary= CV+CC+DJ;
							     if (auxValuesSummary > 0) {//Si-> Compara en las tres matrices y devuleve un unico segmento
							    	 validateEquifaxArrays(ID, Store_group, persona); //Valida deudad 3 matrices
							    	  segment1=(String) persona.get("segment1ExpiredValues");
								      segment2=(String) persona.get("segment2cartValues");
								      segment3=(String) persona.get("segment3lawsuitValues");
								      if(segment1.equals("R")||segment2.equals("R")||segment3.equals("R")) {
								    	  filterEQ="R";
								    	  persona.put("message","Filtro Equifax, Cliente rechazado.");
								      }else {
								    	  if(!statusEquifax) {////////////////////////////////////AQUI EQUIFAX ESTADO
							    				filterEQ=getScoreNewClientDefault();
							    			}else {
									    	  	filterEQ="SP";
										    	validateEquifaxScoreNewClientSP(persona,filterEQ);
								    			filterEQ=(String) persona.get("EQ_scoreClient");
							    			}
								      }
							     } else { //NO-> Compara en la Matriz nuevo Cliente y devuleve un unico segmento
							    	 try {
								      if(!statusEquifax) {////////////////////////////////////AQUI EQUIFAX ESTADO
								    	      filterEQ=getScoreNewClientDefault();
						    			}else {
						    				validateEquifaxScoreNewClient(persona);
//						    				if(Error.equals("")) {
//												OBCriteria<shppee_NewCustomerScore> defaultEQX = OBDal.getInstance().createCriteria(shppee_NewCustomerScore.class);
//												defaultEQX.add(Restrictions.eq(shppee_NewCustomerScore.PROPERTY_SHPPWSDEFAULTEQXSEG, true));
//												shppee_NewCustomerScore defaultEQ = (shppee_NewCustomerScore) defaultEQX.uniqueResult();
//												List<shppee_NewCustomerScore> resultList = defaultEQX.list();
//												if(resultList.size()>0) {
//													persona.put("EQ_scoreClient", defaultEQ.getENDSegment());
//												}
//												
//						    				}
										      filterEQ=(String) persona.get("EQ_scoreClient");
										      if(filterEQ.equals("")) {
										    	  filterEQ="R";
										      }
						    			}
							    	 }catch (Exception e) {}
							     }
					    		}
					    	}else {//proceso para clientes antiguos
						    	if(!statusEquifax) {////////////////////////////////////AQUI EQUIFAX ESTADO
						    		filterEQ = getDefaultRiskIndiex();
						    	}else {
							    	String parallelCreditMatrix=parallelCredits(partner, persona); //Créditos paralelos
							    	filterEQ = customerRiskIndex(partner, parallelCreditMatrix, persona); //Indice de Riesgo
						    	}
					    	}
						}
					    filter=filterEQ;
				    //proceso normal cuando el check de sinergia no sea activo
					}else if(!CheckSinergia) {
			    		Error = procesarApiEQ(partner, accesApi, ID, persona, jsonMonitor); //Aqui obtengo datos de Equifax y guardo en persona
					    String serviceValidate=(String)persona.get("ApiResponse");
					    
					    if(Error != null && !(Error.equals(""))) {
					    	filter = "R";
					    	persona.put("msgLN", Error);
					    	persona.put("idLN",ID);
					    	persona.put("matrizReason","Servicio Equifax");
					    }else {
					    	if(clientnew) {//proceso para Clientes nuevos
					    		if(persona.get("Exception_Institucion").equals("true")) {
					    			filterEQ=(String) persona.get("Segment");
						    		}else {
									CV = (Double) persona.get("CV");
									CC = (Double) persona.get("CC");
									DJ = (Double) persona.get("DJ");
								     Double auxValuesSummary= CV+CC+DJ;
								     if (auxValuesSummary > 0) {//Si-> Compara en las tres matrices y devuleve un unico segmento
								    	 validateEquifaxArrays(ID, Store_group, persona); //Valida deudad 3 matrices
								    	  segment1=(String) persona.get("segment1ExpiredValues");
									      segment2=(String) persona.get("segment2cartValues");
									      segment3=(String) persona.get("segment3lawsuitValues");
									      if(segment1.equals("R")||segment2.equals("R")||segment3.equals("R")) {
									    	  filterEQ="R";
									    	  persona.put("message","Filtro Equifax, Cliente rechazado.");
									      }else {
									    	  	if(!statusEquifax) {////////////////////////////////////AQUI EQUIFAX ESTADO
								    				filterEQ=getScoreNewClientDefault();
								    			}else {
										    	  	filterEQ="SP";
											    	validateEquifaxScoreNewClientSP(persona,filterEQ);
									    			filterEQ=(String) persona.get("EQ_scoreClient");
                                        	  	}
									      }
								     } else { //NO-> Compara en la Matriz nuevo Cliente y devuleve un unico segmento
								    	 try {
								    		 if(!statusEquifax) {////////////////////////////////////AQUI EQUIFAX ESTADO
									    	      filterEQ=getScoreNewClientDefault();
							    			}else {
											      validateEquifaxScoreNewClient(persona);
//											      if(Error.equals("")) {
//														persona.put("EQ_scoreClient", "E2");
//														OBCriteria<shppee_NewCustomerScore> defaultEQX = OBDal.getInstance().createCriteria(shppee_NewCustomerScore.class);
//														defaultEQX.add(Restrictions.eq(shppee_NewCustomerScore.PROPERTY_SHPPWSDEFAULTEQXSEG, true));
//														shppee_NewCustomerScore defaultEQ = (shppee_NewCustomerScore) defaultEQX.uniqueResult();
//														List<shppee_NewCustomerScore> resultList = defaultEQX.list();
//														if(resultList.size()>0) {
//															persona.put("EQ_scoreClient", defaultEQ.getENDSegment());
//														}
//														
//											      }
											      filterEQ=(String) persona.get("EQ_scoreClient");
											      if(filterEQ.equals("")) {
											    	  filterEQ="R";
											      }
							    			}
								    	 }catch (Exception e) {}
								     }
						    		}
							    }else {//proceso para clientes antiguos
							    	if(!statusEquifax) {////////////////////////////////////AQUI EQUIFAX ESTADO
							    		filterEQ = getDefaultRiskIndiex();
							    	}else {
								    	String parallelCreditMatrix=parallelCredits(partner, persona); //Créditos paralelos
								    	filterEQ = customerRiskIndex(partner, parallelCreditMatrix, persona); //Indice de Riesgo
							    	}
						    	}
							}
						    filter=filterEQ;
					}
		    	}else {
		    		if(!persona.get("msgLN").equals("Documento no encontrado en el Registro Civil") && !CustomerExeption_result) {
		    			validateReferencesException( filter, persona,now );
						filter=(String) persona.get("SegmentException");
						persona.put("Mensaje_Operacional","");
						persona.put("Exception_Institucion","false");
				    }else {
				    	Error="No se pudo completar el perfilamiento. No existe información válida en el Registro Civil para continuar con la evaluación del cliente. Por favor, solicite una revisión de los datos.";
				    }
		    	
		    	}
		    
	    
	    
	    
	      ////////////////////////////////EQnewSearch
		 ///Matriz CUPO y Oportunidad /////Finalmente se crea la oportunidad contrastando en la Matriz cupos, solo si no es rechazado
		////////////////////////////////
	    
	    //verifica que el check de sinergia este activo y el estado de sinergia sea diferente de R, si da R se pasara a false
    	if(CheckSinergia && statusSinergia) {
    		String validateCustomer_old_new = validateStatusPartner(response_DI);
    		typeClientSynergy = classificationClient(response_DI);
			if(CustomerExeption_result && validateCustomer_old_new.equals("CLIENTE APROBADO")&&(persona.get("Exception_Institucion").equals("False")||persona.get("Exception_Institucion").equals("false"))) {
				filter=SearchClieSegm(response_DI);
			}
			validateEquifaxQuotas(filter, persona);
			if(clientnew) {
				if(persona.get("Exception_Institucion").equals("true")&&persona.get("EQ_scoreClient").equals("R")) {
	       		 	filter="R";
    				statusSinergia=false;
		   		}else if(persona.containsKey("EQ_scoreClient")){
	        		 filter=(String) persona.get("EQ_scoreClient"); 
		   		}
			}else 
					if(persona.containsKey("EQ_scoreClient")) {
		    			 filter=(String) persona.get("EQ_scoreClient");
		    		 }
				
			Quota_Available= (Double)persona.get("EQ_quota");
			String validateRecover = (String) persona.get("matrizReason");
			if(validateRecover.equals("Matriz Crédito Vigente Vencido")) {
				validatorRC="R";
				statusSinergia=false;
		    	persona.put("message","Filtro Equifax, Cliente rechazado.");
			}
//			if(validateCustomer_old_new.equals("CLIENTE APROBADO")) {
//				filter=SearchClieSegm(response_DI);
//			}	 
			if(!(validateRecover.equals("Recover")) && !validatorRC.equals("R")&& CustomerExeption_result) {
				Error = newOpportunity(filter, persona, accesApi,statusEquifax);////////////////////////////////////AQUI EQUIFAX ESTADO
			}else if(!(validateRecover.equals("Recover")) && validatorRC.equals("R")&& CustomerExeption_result) {
				Error = newOpportunity(filter, persona, accesApi,statusEquifax);////////////////////////////////////AQUI EQUIFAX ESTADO
			}
			if(!(validateRecover.equals("Recover")) && !validatorRC.equals("R")&& !CustomerExeption_result) {
			Error = newOpportunity_Exception(filter, persona, accesApi_exception, accesApi,statusEquifax);////////////////////////////////////AQUI EQUIFAX ESTADO
			} 
    	 }
		//verifica que el check de sinergia este activo y el estado de sinergia sea IGUAL a R, si da R se pasara a false
    	else if(CheckSinergia && !statusSinergia){
   		 validateEquifaxQuotas(filter, persona);
   		 if(clientnew) {
	    		 if(persona.get("Exception_Institucion").equals("true")&&persona.get("EQ_scoreClient").equals("R")) {
	    			 filter="R";
	    		 }else if(persona.containsKey("EQ_scoreClient")){
	        		 filter=(String) persona.get("EQ_scoreClient"); 
	    		 }
   		 }else if(persona.containsKey("EQ_scoreClient")) {
	    			 filter=(String) persona.get("EQ_scoreClient");
   		 }    		 
   		 
   		 Quota_Available= (Double)persona.get("EQ_quota");
   		 String validateRecover = (String) persona.get("matrizReason");
   		 if(!(validateRecover.equals("Recover")) && !validatorRC.equals("R")&& CustomerExeption_result) {
   			 Error = newOpportunity(filter, persona, accesApi,statusEquifax);////////////////////////////////////AQUI EQUIFAX ESTADO
   		 }
   		 if(!(validateRecover.equals("Recover")) && !validatorRC.equals("R")&& !CustomerExeption_result) {
   			 Error = newOpportunity_Exception(filter, persona, accesApi_exception, accesApi,statusEquifax);////////////////////////////////////AQUI EQUIFAX ESTADO
   		 }
		    //proceso normal cuando el check de sinergia no sea activo
   	 }else if(!CheckSinergia){
    		 validateEquifaxQuotas(filter, persona);
    		 if(clientnew) {
	    		 if(persona.get("Exception_Institucion").equals("true")&&persona.get("EQ_scoreClient").equals("R")) {
	    			 filter="R";
	    		 }else if(persona.containsKey("EQ_scoreClient")){
	        		 filter=(String) persona.get("EQ_scoreClient"); 
	    		 }
    		 }else if(persona.containsKey("EQ_scoreClient")) {
	    			 filter=(String) persona.get("EQ_scoreClient");
    		 }    		 
    		 
    		 Quota_Available= (Double)persona.get("EQ_quota");
    		 String validateRecover = (String) persona.get("matrizReason");
    		 if(!(validateRecover.equals("Recover")) && !validatorRC.equals("R")&& CustomerExeption_result) {
    			 Error = newOpportunity(filter, persona, accesApi,statusEquifax);////////////////////////////////////AQUI EQUIFAX ESTADO
    		 }
    		 if(!(validateRecover.equals("Recover")) && !validatorRC.equals("R")&& !CustomerExeption_result) {
    			 Error = newOpportunity_Exception(filter, persona, accesApi_exception, accesApi,statusEquifax);
    		 }
    	 }
		 
		//////////////////////////////////
		//////////REFERENCIAS//////////
		//////////////////////////////////
	
		   
	   
	     //|||||||||||||||||||||||||||||||||||//
		//|||||||||||||RESULTADO EXCEPCION DE CLIENTES|||||||||||||//
	   //|||||||||||||||||||||||||||||||||||//

    		
		    if(CustomerExeption_result){
	
			    validateReferences( filter, persona );
			    String Ref1 = (String) persona.get("Ref1");
			    String Ref2 = (String) persona.get("Ref2");
		    	
		    	
		        //|||||||||||||||||||||||||||||||||||//
				//|||||||||||||RESULTADO|||||||||||||//
			   //|||||||||||||||||||||||||||||||||||//
			    Quota_Available = updateAvailableQuota( filter,  (String)persona.get("OP_documentno"), Quota_Available);
			    
			    String message = Error;//(String) persona.get("message");
			    String matriz = (String) persona.get("matriz");
			        JSONObject respuesta = new JSONObject();
			        respuesta.put("ID", ID);
			        //-------------------------------------------------------------------------------
			        respuesta.put("Segment", filter);
			        respuesta.put("Quota_Available", Quota_Available);
			        respuesta.put("Names", RCname1+" "+RCname2);
			        respuesta.put("Surnames", RClastName1+" "+RClastName2);
			        respuesta.put("Birthday", RCbirthdate);
			        respuesta.put("Gender", RCgender);
			        respuesta.put("Nacionality", RCnationalityAux);
			        respuesta.put("Civil_Status", RCcivilStatus);
			        respuesta.put("Profession", RCprofession);
			        respuesta.put("Type_Entrance", persona.get("Type_Entrance"));
			        respuesta.put("%Entrance", (Double)persona.get("EQ_entrance"));
			        respuesta.put("Deadline", (Double)persona.get("EQ_deadline"));
			        respuesta.put("Type_Ref1", Ref1);
			        respuesta.put("Type_Ref2", Ref2);
			        respuesta.put("No_Opportunity",(String)persona.get("OP_documentno") );
			        respuesta.put("CV", CV);
			        respuesta.put("CC", CC);
			        respuesta.put("DJ", DJ);
			        respuesta.put("segment1", segment1);
			        respuesta.put("segment2", segment2);
			        respuesta.put("segment3", segment3);
			        
			        respuesta.put("EQ_segmentacion", (String)persona.get("EQ_segmentacion"));
			        respuesta.put("EQ_score_inclusion", (Double)persona.get("EQ_score_inclusion"));
			        
			        respuesta.put("message", message);
			        respuesta.put("matriz", matriz);

					if (!persona.containsKey("Mensaje_Operacional")) {
						respuesta.put("status_equifax", "OK");
			        }else if (!persona.get("Mensaje_Operacional").equals("")) {
						respuesta.put("status_equifax", "Error");
			        }
			        if(CheckSinergia && !statusSinergia) {
			        	respuesta.put("status_sinergy_happycel", "R");
			        }
			        if(persona.get("CheckDelivery").equals("true")) {
				        respuesta.put("Venta_a_Domicilio", "Y");
			        }else {
				        respuesta.put("Venta_a_Domicilio", "N");
			        }
			        //respuesta.put("Aux", Aux);
			        
			        response.setContentType("application/json");
			        response.setCharacterEncoding("UTF-8");
	
			        JSONArray jsonArray = new JSONArray();
			        jsonArray.put(respuesta);
			        String json = jsonArray.getJSONObject(0).toString();
			        PrintWriter writer = response.getWriter();
			        writer.write(json);
			        writer.close();
			        jsonMonitor.put("endSHPPWS_SideSoft_Profiling", LocalDateTime.now());
			        
			        String requestUrl = request.getRequestURL().toString();
			        //OBContext baseURL = OBContext.getOBContext();
			        String InterfaceLog = "SHPPWS_NT";
			        String Process = "Perfilamiento";
			        
				String noReference = (String) persona.get("OP_documentno");
				String idRegister = (String) persona.get("OP_record_id");
				if(noReference!=null && idRegister!=null) {
					logger.log_end_register(log, requestUrl, noReference, json, "OK","OUT", InterfaceLog, Process, idRegister, Error);
	
				}else {
					 noReference = "";
					 idRegister = "";
					 logger.log_end_register(log, requestUrl, noReference, json, "ERROR","OUT", InterfaceLog, Process, idRegister, Error);
				}
				
				String OP_documentno = (String)persona.get("OP_documentno");
				if(OP_documentno != null && !OP_documentno.equals("")) {
					jsonMonitor.put("statusSHPPWS_SideSoft_Profiling", "200");
				}else {
					jsonMonitor.put("endSHPPWS_SideSoft_Profiling", "500");
				}
				jsonMonitor.put("Identifier", OP_documentno);
				jsonMonitor.put("Identifier2", ID);
				if(jsonMonitor != null) {
					MonitorManager newMonitor = new MonitorManager();
					newMonitor.sendMonitorData(jsonMonitor, accesApi, true, null);
				}
					
			
		    }else {
		    	
		    	
		    	
		    	 validateReferencesException( filter, persona,now );
				    String Ref3 = (String) persona.get("Ref1");
				    String Ref4 = (String) persona.get("Ref2");
		    	Quota_Available = updateAvailableQuota( filter,  (String)persona.get("OP_documentno"), Quota_Available);
			    String message = Error;//(String) persona.get("message");
			    String matriz = (String) persona.get("matriz");
			        JSONObject respuesta = new JSONObject();
			        respuesta.put("ID", ID);
			        //-------------------------------------------------------------------------------
			        respuesta.put("Segment", accesApi_exception.getFinalsegment());
			        respuesta.put("Quota_Available", accesApi_exception.getQuota());
			        respuesta.put("Names", RCname1+" "+RCname2);
			        respuesta.put("Surnames", RClastName1+" "+RClastName2);
			        respuesta.put("Birthday", RCbirthdate);
			        respuesta.put("Gender", RCgender);
			        respuesta.put("Nacionality", RCnationalityAux);
			        respuesta.put("Civil_Status", RCcivilStatus);
			        respuesta.put("Profession", RCprofession);
			        respuesta.put("Type_Entrance", persona.get("Type_Entrance"));
			        respuesta.put("%Entrance", accesApi_exception.getEntry());
			        respuesta.put("Deadline", accesApi_exception.getMaxterm());
			        respuesta.put("Type_Ref1", Ref3);
			        respuesta.put("Type_Ref2", Ref4);
			        respuesta.put("No_Opportunity",(String)persona.get("OP_documentno") );
			        respuesta.put("CV", 0);
			        respuesta.put("CC", 0);
			        respuesta.put("DJ", 0);
			        respuesta.put("segment1", "");
			        respuesta.put("segment2", "");
			        respuesta.put("segment3", "");
			        
			        respuesta.put("EQ_segmentacion", "");
			        respuesta.put("EQ_score_inclusion", 0);
			        
			        respuesta.put("message", accesApi_exception.getMessage());
			        respuesta.put("matriz", "Matriz excepcion de clientes");
			        if (!persona.containsKey("Mensaje_Operacional")) {
						respuesta.put("status_equifax", "OK");
			        }else if (!persona.get("Mensaje_Operacional").equals("")) {
						respuesta.put("status_equifax", "Error");
			        }
			        if(CheckSinergia && !statusSinergia) {
			        	respuesta.put("status_sinergy_happycel", "R");
			        }else {
			        	respuesta.put("status_sinergy_happycel", "OK");
			        }
			        if(persona.get("CheckDelivery").equals("true")) {
				        respuesta.put("Venta_a_Domicilio", "Y");
			        }else {
				        respuesta.put("Venta_a_Domicilio", "N");
			        }
			        response.setContentType("application/json");
			        response.setCharacterEncoding("UTF-8");
	
			        JSONArray jsonArray = new JSONArray();
			        jsonArray.put(respuesta);
			        String json = jsonArray.getJSONObject(0).toString();
			        PrintWriter writer = response.getWriter();
			        writer.write(json);
			        writer.close();
			        jsonMonitor.put("endSHPPWS_SideSoft_Profiling", LocalDateTime.now());
			        
			        String requestUrl = request.getRequestURL().toString();
			        //OBContext baseURL = OBContext.getOBContext();
			        String InterfaceLog = "SHPPWS_NT";
			        String Process = "Perfilamiento";
			        
				String noReference = (String) persona.get("OP_documentno");
				String idRegister = (String) persona.get("OP_record_id");
				if(noReference!=null && idRegister!=null) {
					logger.log_end_register(log, requestUrl, noReference, json, "OK","OUT", InterfaceLog, Process, idRegister, Error);
	
				}else {
					 noReference = "";
					 idRegister = "";
					 logger.log_end_register(log, requestUrl, noReference, json, "ERROR","OUT", InterfaceLog, Process, idRegister, Error);
				}
				
				String OP_documentno = (String)persona.get("OP_documentno");
				if(OP_documentno != null && !OP_documentno.equals("")) {
					jsonMonitor.put("statusSHPPWS_SideSoft_Profiling", "200");
				}else {
					jsonMonitor.put("endSHPPWS_SideSoft_Profiling", "500");
				}
				jsonMonitor.put("Identifier", OP_documentno);
				jsonMonitor.put("Identifier2", ID);
				if(jsonMonitor != null) {
					MonitorManager newMonitor = new MonitorManager();
					newMonitor.sendMonitorData(jsonMonitor, accesApi, true, null);
				}
					
		    }
    	}
	
    	
    	
	

	
	public void doPost(String path, HttpServletRequest request, HttpServletResponse response) throws Exception {
		// TODO Auto-generated method stub
		
	}

	
	public void doDelete(String path, HttpServletRequest request, HttpServletResponse response) throws Exception {
		// TODO Auto-generated method stub
		
	}

	
	public void doPut(String path, HttpServletRequest request, HttpServletResponse response) throws Exception {
		// TODO Auto-generated method stub
	}
	
	
	/*
	**Se actualiza la cuota disponible para el usuario
	*/
	public Double updateAvailableQuota(String filter, String numOportunity, Double Quota_Available) {
		if(numOportunity!=null && !(numOportunity.equals(""))) {
			OBCriteria<Opcrmopportunities> queryOp= OBDal.getInstance().createCriteria(Opcrmopportunities.class);
			queryOp.add(Restrictions.eq(Opcrmopportunities.PROPERTY_SHPPWSOPDOCUMENTNO,numOportunity));
			List<Opcrmopportunities> listOp =  queryOp.list();
			if(listOp.size()>0 && !(filter.equals("R")) ) {
				BusinessPartner partner = listOp.get(0).getBusinessPartner();
				BigDecimal quotaProfiling = listOp.get(0).getOpportunityAmount();
				if(partner!= null && quotaProfiling != null) {
					partner.setShppwsProfiledQuota(quotaProfiling);
					OBDal.getInstance().save(partner);
					OBDal.getInstance().flush();
					OBDal.getInstance().refresh(partner);
					Quota_Available = partner.getShppwsAvailableQuota().doubleValue();
				}
			}
		}
		return Quota_Available;
	}
	
	
	//
	//Se obtiene la respuesta de cada servicio Json
	//
	
	public String getApiResponse(shppws_config accesApi, String Identifier, int filternumber, JSONObject jsonMonitor) throws Exception {
		log_records logger = new log_records();
		
		String apiUrl="";
	    String apiEndPoint="";
	    String apiTypeAuth="";
	    String apiUser="";
	    String apiPass="";
	    String apiToken="";
	    
	    // Aplican para equifax
	    String apiScope=""; 
	    String apiTokenURL = "";
	    
	    String Depurador="";
	    
	    String Interface = "";
	    String Process = "Externo";
	    String idRegister = "";
	    String Error = "";
	    String nameService="";
	    String messageErrorService="";
	    String apiTokenPass="";

		boolean statusSinergia = true;
		boolean statusEquifax = true;
	    
		
		if(filternumber == 1) {
		Interface="SHPPWS_RC";
	     apiUrl=accesApi.getRCNamespace();
	     apiEndPoint=accesApi.getRCReadEndpoint();
	     apiTypeAuth=accesApi.getRCTypeAuth();
	     apiUser=accesApi.getRCUser();
	     apiPass=accesApi.getRCPass();
	     apiToken=accesApi.getRCToken();
	     nameService = "Registro Civil";
	     messageErrorService = accesApi.getRecoverMessageError();
		}else if(filternumber == 2){
			Interface="SHPPWS_LN_CI";
			 apiUrl=accesApi.getLN1Namespace();
		     apiEndPoint=accesApi.getLN1ReadEndpoint();
		     apiTypeAuth=accesApi.getLN1TypeAuth();
		     apiUser=accesApi.getLN1User();
		     apiPass=accesApi.getLN1Pass();
		     apiToken=accesApi.getLN1Token();
		     nameService = "Listas negras Cédula";
		     messageErrorService = accesApi.getLN1MessageError();
		}else if(filternumber == 3){
			Interface="SHPPWS_LN_TLF";
			 apiUrl=accesApi.getLN2Namespace();
		     apiEndPoint=accesApi.getLN2ReadEndpoint();
		     apiTypeAuth=accesApi.getLN2TypeAuth();
		     apiUser=accesApi.getLN2User();
		     apiPass=accesApi.getLN2Pass();
		     apiToken=accesApi.getLN2Token();
		     nameService = "Listas negras Teléfonos";
		     messageErrorService = accesApi.getLN2MessageError();
		}else if(filternumber == 4){
			Interface="SHPPWS_LN_C";
			 apiUrl=accesApi.getLN3Namespace();
		     apiEndPoint=accesApi.getLN3ReadEndpoint();
		     apiTypeAuth=accesApi.getLN3TypeAuth();
		     apiUser=accesApi.getLN3User();
		     apiPass=accesApi.getLN3Pass();
		     apiToken=accesApi.getLN3Token();
		     nameService = "Listas negras Correos";
		     messageErrorService = accesApi.getLN3MessageError();
		}else if(filternumber == 5){
			Interface="SHPPWS_LN_DI";
			 apiUrl=accesApi.getNamespace();
		     apiEndPoint=accesApi.getReadEndpoint();
		     apiTypeAuth=accesApi.getTypeAuth();
		     apiUser=accesApi.getUser();
		     apiPass=accesApi.getPass();
		     apiToken=accesApi.getToken();
		     nameService = "Listas negras Deudas Internas";
		     messageErrorService = accesApi.getLndiMessageError();
		}else if(filternumber == 7){
			Interface="SHPPWS_EQ";
			 apiUrl=accesApi.getEQNamespace();
		     apiEndPoint=accesApi.getEQReadEndpoint();
		     apiTypeAuth=accesApi.getEQTypeAuth();
		     apiUser=accesApi.getEQUser();
		     apiPass=accesApi.getEQPass();
		     apiTokenURL=accesApi.getEQToken(); // URL de autenticacion de Equifax
		     apiScope=accesApi.getEQParams(); // Scopes de Equifax
		     apiToken=getEquifaxToken(apiTokenURL, apiScope, apiUser, apiPass);
		     nameService = "Equifax";
		     messageErrorService = accesApi.getEquifaxMessageError();
		     Process = "Equifax";
		}else if(filternumber == 8){
			Interface="SHPPWS_SN_HC";
			 apiUrl=accesApi.getSynergyNameSpace();
		     apiEndPoint=accesApi.getSynergyReadLastPoint();
		     apiTypeAuth=accesApi.getSynergyAuthenticType();
		     apiUser=accesApi.getSynergyUser();
		     apiPass=accesApi.getSynergyKey();
		     apiToken=accesApi.getSynergyToken();
		     apiTokenPass=accesApi.getSynergyTokenPass();
		     nameService = "Sinergia - Happycel";
		     messageErrorService = accesApi.getSynergyMessageError();
		     idRegister="CEDULA";
		}
		Scactu_Log log = logger.log_start_register(accesApi, apiEndPoint, null);
	    int responseCode = 500;
	    HttpURLConnection connectionhttp=null;
	    HttpsURLConnection connectionhttps=null;
	    HttpURLConnection connection = null;
	    JSONObject requestBody = new JSONObject();
	    
	    jsonMonitor.put(Interface, "Service"+filternumber);
	    jsonMonitor.put("start"+Interface, LocalDateTime.now());
	    jsonMonitor.put("type"+Interface, nameService);

	    //BA -> Basic auth
	    //TA -> Token auth
			    if (apiTypeAuth.equals("BA")) {
					    	URL url = new URL(apiUrl + apiEndPoint);
						     connectionhttp = (HttpURLConnection) url.openConnection();
						     connectionhttp.setRequestMethod("GET");
						    String username = apiUser;
						    String password = apiPass;
						    String authString = username + ":" + password;
						    String authHeaderValue = "Basic " + Base64.getEncoder().encodeToString(authString.getBytes());
						    connectionhttp.setRequestProperty("Authorization", authHeaderValue);
						    
				 // Obtiene la respuesta de la API
				     responseCode = connectionhttp.getResponseCode();
				     connection = connectionhttp;
			    }else if (apiTypeAuth.equals("AT")) {
					    	// Deshabilitar la validación de certificados SSL
						    TrustManager[] trustAllCerts = new TrustManager[] {
						        new X509TrustManager() {
						            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
						                return null;
						            }
						            public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
						            }
						            public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
						            }
						        }
						    };
							
							 // Configurar SSLContext con la configuración personalizada
							     SSLContext sslContext = SSLContext.getInstance("TLS");
							     sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
							     // Obtener la conexión HTTPS y aplicar la configuración personalizada
							     URL url = new URL(apiUrl + apiEndPoint);
							     if(filternumber != 8) {
							    	 connectionhttps = (HttpsURLConnection) url.openConnection();
								     // Desactivar la verificación estricta del nombre del host
								     connectionhttps.setHostnameVerifier((hostname, session) -> true);
								     connectionhttps.setSSLSocketFactory(sslContext.getSocketFactory());
								     String typeEndpoint = "POST";
									 connectionhttps.setRequestMethod(typeEndpoint);


							     //String token = apiToken;
									 String token = apiToken;
								     String authHeaderValue = "Bearer " + token;
								     connectionhttps.setRequestProperty("Authorization", authHeaderValue);
							    	
							     						    	
							     
							     if(filternumber == 1) {
								    	requestBody.put("ci", Identifier);
								    	requestBody.put("apikey", apiToken);
								    	Depurador=Depurador + "Construye el body para RC";
							     }else if(filternumber == 2) {
								    	requestBody.put("Cedula_Cliente", Identifier);
								    	requestBody.put("Key", apiToken);
								    	Depurador=Depurador + "Construye el body para LN1";
							     }else if(filternumber == 3) {
							    	 try {
							    		 String ultimosNueveDigitos = Identifier.substring(Identifier.length() - 9);
								    	 String numeroCompleto = "0" + ultimosNueveDigitos;
								    	 Identifier = numeroCompleto;
							    	 }catch(Exception e) {}
								    	requestBody.put("Telefono_Cliente", Identifier);
								    	requestBody.put("Key", apiToken);
								    	Depurador=Depurador + "Construye el body para LN2";
							     }else if(filternumber == 4) {
								    	requestBody.put("Correo_Cliente", Identifier);
								    	requestBody.put("Key", apiToken);
								    	Depurador=Depurador + "Construye el body para LN3";
							     }else if(filternumber == 5) {
								    	requestBody.put("Cedula_Cliente", Identifier);
								    	requestBody.put("Key", apiToken);
								    	Depurador=Depurador + "Construye el body para DI";
							     }else if(filternumber == 7){
							    	 	JSONObject personalInformation = new JSONObject();
							    	    personalInformation.put("tipoDocumento", "C"); 
							    	    personalInformation.put("numeroDocumento", Identifier);

							    	    JSONObject primaryConsumer = new JSONObject();
							    	    primaryConsumer.put("personalInformation", personalInformation);

							    	    JSONObject applicants = new JSONObject();
							    	    applicants.put("primaryConsumer", primaryConsumer);

							    	    // Sección nueva para productData
							    	    JSONObject productData = new JSONObject();
							    	    productData.put("billTo", accesApi.getBillTo());
							    	    productData.put("shipTo", accesApi.getShipTo());
							    	    productData.put("configuration", accesApi.getConfiguration());
							    	    productData.put("customer", accesApi.getCustomerName());
							    	    productData.put("model", accesApi.getModelName());
							    	    requestBody.put("applicants", applicants);
							    	    requestBody.put("productData", productData);

							    	    Depurador = Depurador + " Construye el body para EQ " + requestBody.toString();
								}
						    	
						    	
						        log = logger.log_setValues(log, requestBody.toString());
						    	connectionhttps.setRequestProperty("Content-Type", "application/json");	

						    	connectionhttps.setDoOutput(true);
						    	OutputStreamWriter writer = new OutputStreamWriter(connectionhttps.getOutputStream());
						    	writer.write(requestBody.toString());
						    	writer.flush();
						    	writer.close();
						    	
						      	Depurador=Depurador + "Obtiene la respuesta ";
						    	// Obtiene la respuesta de la API
							     responseCode = connectionhttps.getResponseCode();
							     connection = connectionhttps;
							     }else {
							    	 
							    	 
							    	 
							    	 connectionhttp = (HttpURLConnection) url.openConnection(); 
							     String typeEndpoint = "POST";
							     //if(filternumber == 8) {
							    	 url = new URL(url + "?Documento=" + Identifier);
								     typeEndpoint = "GET";
								     connectionhttp = (HttpURLConnection) url.openConnection();
									 connectionhttp.setRequestMethod(typeEndpoint);

								     //String token = apiToken;
									 String apiUrlToken = apiToken;
								     String basicHeaderToken = apiTokenPass;
									 String token = GenerateToken(apiUrlToken, basicHeaderToken);
								     String authHeaderValue = "Bearer " + token;
								     connectionhttp.setRequestProperty("Bearer", token);
								     connectionhttp.setRequestProperty("Accept", "application/json");	
								     requestBody.put("identificacion", Identifier);
								     String RefernceNo = "Cedula: "+Identifier;
							    	 Identifier = RefernceNo;
							    	 Process="Consulta Información de Cliente";
								     log = logger.log_setValues(log, requestBody.toString());

								     Depurador=Depurador + "Obtiene la respuesta ";
								    	// Obtiene la respuesta de la API
									     responseCode = connectionhttp.getResponseCode();
									     connection = connectionhttp;
								 
							    	 
								 
								
						}
				  
				    }
				    jsonMonitor.put("status"+Interface, responseCode);
				    jsonMonitor.put("end"+Interface, LocalDateTime.now());

				    
				    
				    
				    if (responseCode == HttpURLConnection.HTTP_OK) {
				        // S lee la respuesta de la API
				        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				        StringBuilder responseBuilder = new StringBuilder();
				        String line;
				        while ((line = reader.readLine()) != null) {
				            responseBuilder.append(line);
				        }
				        reader.close();

				        // Se retorna el string de la api
				        logger.log_end_register(log, apiUrl, Identifier, responseBuilder.toString(), "OK", "IN", Interface,  Process,  idRegister, Error);
				        return responseBuilder.toString();
				    } else {
				    	if(filternumber == 8) {
				    		statusSinergia = false;
				    		Error="Error en la consulta a la API "+nameService+" Código de respuesta: " + responseCode;
					        logger.log_end_register(log, apiUrl, Identifier, "Response Code "+responseCode, "ERROR", "IN", Interface, Process, idRegister, Error);
					        return "Error sinergia";  
				    	}else if(filternumber == 7) {
				    		statusSinergia = false;
				    		statusEquifax = false;
				    		Error="Error en la consulta a la API "+nameService+" Código de respuesta: " + responseCode;
					        logger.log_end_register(log, apiUrl, Identifier, "Response Code "+responseCode, "ERROR", "IN", Interface, Process, idRegister, Error);
					        return "Error equifax";  
				    	}
				    	if(filternumber != 7 && filternumber != 8) {
					    	Error="Error en la consulta a la API "+nameService+" Código de respuesta: " + responseCode;
					        logger.log_end_register(log, apiUrl, Identifier, "Response Code "+responseCode, "ERROR", "IN", Interface, Process, idRegister, Error);
					        throw new Exception("Error "+ responseCode +" "+messageErrorService);
				    	}
				    	return null;
				    }
				}	
	
	public static String GenerateToken(String apiUrl, String basicHeader) {
	    HttpURLConnection connection = null;
	    try {
	        // Crear la URL y abrir la conexión
	        URL url = new URL(apiUrl);
	        connection = (HttpURLConnection) url.openConnection();

	        // Configurar la conexión
	        connection.setRequestMethod("POST");
	        connection.setRequestProperty("basic", basicHeader); 
	        connection.setRequestProperty("Content-Type", "application/json");
	        connection.setDoOutput(true);

	        // Si no necesitas body, puedes omitir este bloque
	        try (DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream())) {
	            outputStream.writeBytes(""); // cuerpo vacío
	            outputStream.flush();
	        }

	        // Leer la respuesta
	        int responseCode = connection.getResponseCode();
	        BufferedReader reader;
	        if (responseCode >= 200 && responseCode < 300) {
	            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
	        } else {
	            reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
	        }

	        StringBuilder response = new StringBuilder();
	        String line;
	        while ((line = reader.readLine()) != null) {
	            response.append(line);
	        }
	        reader.close();

	        // Extraer el valor de "data" usando regex
	        String responseText = response.toString();
	        Pattern pattern = Pattern.compile("\"data\"\\s*:\\s*\"(.*?)\"");
	        Matcher matcher = pattern.matcher(responseText);

	        if (matcher.find()) {
	            return matcher.group(1); // El token
	        } else {
	            return "No se encontró el campo 'data' en la respuesta: " + responseText;
	        }

	    } catch (Exception e) {
	        e.printStackTrace();
	        return "Error: " + e.getMessage();
	    } finally {
	        if (connection != null) {
	            connection.disconnect();
	        }
	    }
	}
	
	
	// Generacion de token para API Equifax
	public String getEquifaxToken(String tokenURL, String scope, String user, String password) throws Exception {
	    // Reutilizar token si es válido
	    if (cachedToken != null && System.currentTimeMillis() < tokenExpiryTime) {
	        return cachedToken;
	    }

	    // Construir cuerpo URL-encoded
	    String body = "grant_type=client_credentials&scope=" + URLEncoder.encode(scope, "UTF-8");

	    URL url = new URL(tokenURL);
	    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	    conn.setRequestMethod("POST");
	    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

	    // Basic Auth
	    String authString = user + ":" + password;
	    String encodedAuth = Base64.getEncoder().encodeToString(authString.getBytes(StandardCharsets.UTF_8));
	    conn.setRequestProperty("Authorization", "Basic " + encodedAuth);

	    conn.setDoOutput(true);
	    try (OutputStream os = conn.getOutputStream()) {
	        os.write(body.getBytes(StandardCharsets.UTF_8));
	    }

	    int responseCode = conn.getResponseCode();
	    if (responseCode != HttpURLConnection.HTTP_OK) {
	        try (BufferedReader errorReader = new BufferedReader(
	                new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
	            StringBuilder errorResponse = new StringBuilder();
	            String line;
	            while ((line = errorReader.readLine()) != null) {
	                errorResponse.append(line.trim());
	            }
	            log4j.error("Error al generar token Equifax. Código: " + responseCode + ", " + errorResponse);
	            throw new RuntimeException("Error al generar token Equifax. Código: " + responseCode + ", " + errorResponse);
	        }
	    }

	    StringBuilder response = new StringBuilder();
	    try (BufferedReader reader = new BufferedReader(
	            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
	        String line;
	        while ((line = reader.readLine()) != null) {
	            response.append(line.trim());
	        }
	    }

	    JSONObject jsonResponse = new JSONObject(response.toString());
	    cachedToken = jsonResponse.getString("access_token");
	    int expiresInSecs = jsonResponse.getInt("expires_in");

	    tokenExpiryTime = System.currentTimeMillis() + (expiresInSecs * 1000L);

	    return cachedToken;
	}

	//
	 //Primer FILTRO
	//
	public void procesarApiRC(String apiResponse, List<shppev_age> reasonAges, String Store_group,  Map<String, Object> personaRC) throws JSONException {
		// Converts the JSON string to a JSONObject
	    JSONObject jsonResponse = new JSONObject(apiResponse);

	    // Access the "persona" field
	    JSONObject persona = jsonResponse.getJSONObject("persona");

	    // Access the "datos" field
	    JSONObject datos = persona.getJSONObject("datos");

	    // Access the individual fields
	    String name1=datos.getString("nombreprimero");
	    String name2=datos.getString("nombresegundo");
	    String name3=datos.getString("nombretercero");
	    String name4=datos.getString("nombrecuarto");
	    String name5=datos.getString("nombrequinto");
	    
	    if(name2.isEmpty() && name3.isEmpty() && name4.isEmpty() && name5.isEmpty()) {
	    	personaRC.put("RCname1", name1);
	    	personaRC.put("RCname2", "");
	    }else if(name3.isEmpty() && name4.isEmpty() && name5.isEmpty()) {
	    	personaRC.put("RCname1", name1);
	    	personaRC.put("RCname2", name2);
	    }else if( name4.isEmpty() && name5.isEmpty()) {
	    	personaRC.put("RCname1", name1 +" "+name2);
	    	personaRC.put("RCname2", name3);
	    }else if(name5.isEmpty()) {
	    	personaRC.put("RCname1", name1 +" "+name2);
	    	personaRC.put("RCname2", name3 +" "+name4);
	    }else {
	    	personaRC.put("RCname1", name1 +" "+name2+" "+name3);
	    	personaRC.put("RCname2", name4 +" "+name5);
	    }
	    
	    personaRC.put("RCnames", datos.getString("nombres"));
	    personaRC.put("RClastName1", datos.getString("apellidopaterno"));
	    personaRC.put("RClastName2", datos.getString("apellidomaterno"));
	    //personaRC.put("RCname1", datos.getString("nombreprimero"));
	    //personaRC.put("RCname2", datos.getString("nombresegundo"));
	    String RCgender = datos.getString("cod_sexo");
	    if(RCgender.equals("HOMBRE")) {
	    	personaRC.put("RCgender", "MASCULINO");
	    }else if(RCgender.equals("MUJER")) {
	    	personaRC.put("RCgender", "FEMENINO");
	    }else {
	    	personaRC.put("RCgender", "OTRO");
	    }
	    personaRC.put("RCbirthdate", datos.getString("fecha_nacimiento"));
	    personaRC.put("RCprofession", datos.getString("cod_profesion"));
	    personaRC.put("RCcivilStatus", datos.getString("cod_estado_civil"));
	    personaRC.put("RCnationality", datos.getString("nacionalidad"));
	    personaRC.put("RCcedula", datos.getString("cedula"));
	    String fecha_fallecimiento = datos.getString("fecha_fallecimiento");
	    personaRC.put("RCfallecimiento", fecha_fallecimiento);
	    personaRC.put("filterRC", "R");
	    personaRC.put("matrizReason","Matriz Edad");
    	personaRC.put("idLN","Matriz Edad");
    	personaRC.put("msgLN","No encontrado");
    	
	    try {
	    	String RCcivilStatus = (String)personaRC.get("RCcivilStatus");
	    	OBCriteria<CivilStatus> queryCivilStatus= OBDal.getInstance().createCriteria(CivilStatus.class);
	  		queryCivilStatus.add(Restrictions.eq(CivilStatus.PROPERTY_COMMERCIALNAME, RCcivilStatus));
	  		CivilStatus objCivilStatus = (CivilStatus)queryCivilStatus.uniqueResult();
	  		personaRC.put("RCcivilStatus", objCivilStatus.getValue());
	    }catch(Exception e) {}
	    
	    LocalDate fechaActual = LocalDate.now();
	    String nombres = datos.has("nombres") && datos.getString("nombres") != null ? datos.getString("nombres"):"";
		String fechaNacimientoTexto = datos.getString("fecha_nacimiento");
		if (!fechaNacimientoTexto.equals("ND") && !nombres.equals("Documento inexistente")) {
			// Define el formato de la fecha
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/M/yyyy");

			// Convert fecha de nacimiento en LocalDate
			LocalDate fechaNacimiento = LocalDate.parse(fechaNacimientoTexto, formatter);

			// Calcula la diferencia entre la fecha de nacimiento y la fecha actual
			Period diferencia = Period.between(fechaNacimiento, fechaActual);

			// Calcula la edad en años con parte decimal para meses y días
			double auxAge = diferencia.getYears() + ((double) diferencia.getMonths() / 12)
					+ ((double) diferencia.getDays() / 365);
			BigDecimal auxAgeBigDecimal = new BigDecimal(auxAge);
			personaRC.put("auxAgeBigDecimal", auxAgeBigDecimal);
			// Comparación de campos "shop_group" y "id"
			String shopGroup = Store_group;
			Boolean validateDefaultRC = true;
			for (shppev_age reasonAge : reasonAges) {
				if (shopGroup.equals(reasonAge.getShopGroup())) {
					BigDecimal ageInitial = reasonAge.getInitialAge();
					BigDecimal ageFinal = reasonAge.getFinalAge();
					String answer = reasonAge.getAnswer();
					if (auxAgeBigDecimal.compareTo(ageInitial) > 0 && auxAgeBigDecimal.compareTo(ageFinal) < 0) {
						validateDefaultRC = false;
						personaRC.put("matrizReason", "Matriz Edad");
						personaRC.put("idLN", auxAge + "");
						personaRC.put("msgLN", reasonAge.getMessage() + "");
						personaRC.put("filterRC", answer);
					}
					// break; // Si se encuentra una coincidencia, se detiene la iteración
				}
			}
			if (validateDefaultRC) {
				for (shppev_age deafultObj : reasonAges) {
					if (deafultObj.isShppwsDefaultField()) {
						personaRC.put("matrizReason", "Matriz Edad");
						personaRC.put("idLN", auxAge + "");
						personaRC.put("msgLN", deafultObj.getShppwsDefaultMessage() + "");
						personaRC.put("filterRC", deafultObj.getAnswer());
					}
				}
			}
			personaRC.put("message", " Filtro No 1, documento encontrado en el Registro Civil.");
			personaRC.put("matriz", " Motivo Edad");
			BigDecimal bigDecimalAge = BigDecimal.valueOf(auxAge);
			bigDecimalAge = bigDecimalAge.setScale(2, RoundingMode.HALF_UP);

			String auxFilter = (String) personaRC.get("filterRC");
			if (auxFilter != null && auxFilter.equals("C")) {
				personaRC.put("matrizReason", "Motivos de Estado de Vida");

				// ESTADOS DE VIDA
				shppev_lifestyleReason objLifeStyle = new shppev_lifestyleReason();
				if (fecha_fallecimiento != null && !fecha_fallecimiento.equals("") && !fecha_fallecimiento.equals("null")) {
					OBCriteria<shppev_lifestyleReason> queryLifeStyleReason = OBDal.getInstance()
							.createCriteria(shppev_lifestyleReason.class);// Estado de Vida
					queryLifeStyleReason.add(Restrictions.eq(shppev_lifestyleReason.PROPERTY_LIFESTYLE, "D"));
					List<shppev_lifestyleReason> listLifeStyleReason = queryLifeStyleReason.list();
					objLifeStyle = listLifeStyleReason.get(0);
					personaRC.put("matrizReason", "Motivos de Estado de Vida");
					personaRC.put("msgLN", objLifeStyle.getMessage() + "");
					personaRC.put("idLN", fecha_fallecimiento);
					personaRC.put("filterRC", objLifeStyle.getAnswer());
				} else {
					OBCriteria<shppev_lifestyleReason> queryLifeStyleReason = OBDal.getInstance()
							.createCriteria(shppev_lifestyleReason.class);
					queryLifeStyleReason.add(Restrictions.eq(shppev_lifestyleReason.PROPERTY_LIFESTYLE, "A"));
					List<shppev_lifestyleReason> listLifeStyleReason = queryLifeStyleReason.list();
					objLifeStyle = listLifeStyleReason.get(0);
					personaRC.put("matrizReason", "Motivos de Estado de Vida");
					personaRC.put("msgLN", objLifeStyle.getMessage() + "");
					personaRC.put("idLN", fechaNacimientoTexto);
					personaRC.put("filterRC", objLifeStyle.getAnswer());
				}

				// DEFAULT
				if (objLifeStyle.getId() == null || objLifeStyle.getId().equals("")) {
					OBCriteria<shppev_lifestyleReason> queryLifeStyleReason = OBDal.getInstance()
							.createCriteria(shppev_lifestyleReason.class);
					queryLifeStyleReason.add(Restrictions.eq(shppev_lifestyleReason.PROPERTY_SHPPWSDEFAULTFIELD, true));
					List<shppev_lifestyleReason> listLifeStyleReason = queryLifeStyleReason.list();
					if (listLifeStyleReason.size() > 0) {
						objLifeStyle = listLifeStyleReason.get(0);
						personaRC.put("matrizReason", "Motivos de Estado de Vida");
						personaRC.put("msgLN", objLifeStyle.getShppwsDefaultMessage() + "");
						personaRC.put("idLN", fechaNacimientoTexto);
						personaRC.put("filterRC", objLifeStyle.getAnswer());
					}
				}

			}

		} else {
			personaRC.put("message", " Filtro No 1, documento no encontrado en el Registro Civil.");
			personaRC.put("matriz", " Registro Civil");
			personaRC.put("matrizReason", "Documento no encontrado");
			personaRC.put("idLN", "Sin datos en Registro civil");
			personaRC.put("msgLN", "Documento no encontrado en el Registro Civil");
			personaRC.put("filterRC", "R");
			personaRC.put("validatorRC", "R");
		}

		String RCnationality = (String) personaRC.get("RCnationality");
		if (!RCnationality.equals("ECUATORIANA")) {
			personaRC.put("RCnationalityAux", "EXTRANJERO");
		} else {
			personaRC.put("RCnationalityAux", "ECUATORIANA");
		}
	}
	
	
	 //
	  //Verifica existencia de tercero
	 //
	public BusinessPartner validatePartner(shppws_config accesApi, String ID, Map<String, Object> persona )throws Exception {
   // Se verifica existencia de un Tercero 
      BusinessPartner partner = OBProvider.getInstance().get(BusinessPartner.class);
	    String partnerID="";
       try {
		    OBCriteria<BusinessPartner> querypartner= OBDal.getInstance().createCriteria(BusinessPartner.class);
		    querypartner.add(Restrictions.eq(BusinessPartner.PROPERTY_SEARCHKEY, ID));
		    BusinessPartner alreadypartner = (BusinessPartner) querypartner.uniqueResult();
		    partnerID = alreadypartner.getId();
		    partner=alreadypartner;
      }catch(Exception e) {
			System.err.println("Error en EQnewSearch: " + e.getMessage());
		}
       
	    try {
			    if (partnerID.equals("")) {//Me crea un nuevo tercero
			        partner.setClient(accesApi.getClient());
			        partner.setOrganization(accesApi.getOrganization());
			        partner.setActive(accesApi.isActive());
			        partner.setCreatedBy(accesApi.getCreatedBy());
			        partner.setUpdatedBy(accesApi.getUpdatedBy());
			        
			        partner.setSearchKey(ID); //identificador
			        partner.setName((String)persona.get("RCname1")); //FiscalName
			        partner.setName2((String)persona.get("RCname2"));
				    partner.setTaxID(ID);
				    BigDecimal bd = new BigDecimal("0");
			        partner.setCreditLimit(bd); 
			        partner.setSsscrbpName((String) persona.get("RCname1"));
			        partner.setSsscrbpName2((String) persona.get("RCname2"));
			        partner.setSSSCRBPLastname((String)persona.get("RClastName1"));
			        partner.setSsscrbpLastname2((String)persona.get("RClastName2"));
			        
			        partner.setBusinessPartnerCategory(accesApi.getBusinessPartnerCategory());
			        
			        
			        partner.setSsscrbpTypeOfTaxpayer(accesApi.getTaxpayer());
			        partner.setSswhTaxidtype(accesApi.getTypeID());
			        partner.setSSWHTaxpayer(accesApi.getTaxpayerType());
			        
			        //cliente
			        partner.setCustomer(accesApi.isCustomer());
			        partner.setPriceList(accesApi.getPriceList());
			        partner.setPaymentMethod(accesApi.getPaymentMethod());
			        partner.setPaymentTerms(accesApi.getPaymentTerms());
			        partner.setShppwsNationality((String)persona.get("RCnationalityAux"));
			        partner.setEEIEmail((String)persona.get("email"));
			        
			        String dateBirth = (String)persona.get("RCbirthdate");
			        DateTimeFormatter formatoEntrada = DateTimeFormatter.ofPattern("d/M/yyyy");
			        LocalDate localDate = LocalDate.parse(dateBirth, formatoEntrada);
			        Date newDate = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
			        partner.setSbpcDatebirth(newDate);
			        
			        
				OBDal.getInstance().save(partner);
				OBDal.getInstance().flush();
					persona.put("partnerID",partner.getId());
			    }else {//Me actualiza el Tercero existente
			    	partner.setClient(accesApi.getClient());
			        partner.setOrganization(accesApi.getOrganization());
			        partner.setActive(accesApi.isActive());
			        partner.setCreatedBy(accesApi.getCreatedBy());
			        partner.setUpdatedBy(accesApi.getUpdatedBy());
			        
			        partner.setSearchKey(ID); //identificador
			        partner.setName((String)persona.get("RCname1")); //FiscalName
			        partner.setName2((String)persona.get("RCname2"));
				    partner.setTaxID(ID);
				    BigDecimal bd = new BigDecimal("0");
			        partner.setCreditLimit(bd); 
			        partner.setSsscrbpName((String) persona.get("RCname1"));
			        partner.setSsscrbpName2((String) persona.get("RCname2"));
			        partner.setSSSCRBPLastname((String)persona.get("RClastName1"));
			        partner.setSsscrbpLastname2((String)persona.get("RClastName2"));
			        partner.setSsscrbpTypeOfTaxpayer(accesApi.getTaxpayer());
			        partner.setBusinessPartnerCategory(accesApi.getBusinessPartnerCategory());
			        partner.setSswhTaxidtype(accesApi.getTypeID());
			        partner.setSSWHTaxpayer(accesApi.getTaxpayerType());
			        partner.setCustomer(accesApi.isCustomer());
			        partner.setPriceList(accesApi.getPriceList());
			        
			      //cliente
			        partner.setCustomer(accesApi.isCustomer());
			        partner.setPriceList(accesApi.getPriceList());
			        partner.setPaymentMethod(accesApi.getPaymentMethod());
			        partner.setPaymentTerms(accesApi.getPaymentTerms());
			        partner.setShppwsNationality((String)persona.get("RCnationalityAux"));
			        //partner.setEEIEmail((String)persona.get("email"));
			        
			        
			        String dateBirth = (String)persona.get("RCbirthdate");
			        DateTimeFormatter formatoEntrada = DateTimeFormatter.ofPattern("d/M/yyyy");
			        LocalDate localDate = LocalDate.parse(dateBirth, formatoEntrada);
			        Date newDate = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
			        partner.setSbpcDatebirth(newDate);
			        
				    OBDal.getInstance().save(partner);
					OBDal.getInstance().flush();
					persona.put("partnerID",partner.getId());
						
						
			    }
			    
				    OBCriteria<Location> queryLocation = OBDal.getInstance().createCriteria(Location.class);
				    queryLocation.add(Restrictions.eq(Location.PROPERTY_BUSINESSPARTNER, partner));
				    List<Location> objLocation = queryLocation.list();
				    Location address = OBProvider.getInstance().get(Location.class);
			    	if(objLocation.size() <= 0) {
			    		//Dirección
						address.setClient(accesApi.getClient());
						address.setOrganization(accesApi.getOrganization());
						address.setActive(accesApi.isActive());
						address.setCreatedBy(accesApi.getCreatedBy());
						address.setUpdatedBy(accesApi.getUpdatedBy());
						address.setBusinessPartner(partner);
						address.setName("Default Location");
						address.setScactuCellphoneNumber((String)persona.get("CellPhone"));
						address.setPhone((String)persona.get("CellPhone"));
			    	OBDal.getInstance().save(address);
					OBDal.getInstance().flush();
			    }
	    }catch(Exception e) {
			throw new Exception("No se ha podido procesar la información del tercero");
		}
			return partner;
}
	
	
	  //
	 //Filter blacklist
	//
	public boolean validateLN_CI( String ID, String apiResponse,Map<String, Object> persona)throws JSONException {
		JSONArray arrayBlacklist = new JSONArray(apiResponse);
		Boolean validate_CI = true;
		
		for (int i = 0; i < arrayBlacklist.length(); i++) {
			JSONObject objBlacklist = arrayBlacklist.getJSONObject(i);
			String auxValidate = objBlacklist.getString("motivo");
			if (auxValidate.equals("No existe motivo.") || auxValidate.equals("ERROR KEY")) {
				validate_CI = true;
			} else {
				try {
					OBCriteria<shppev_rIdentification> queryReason = OBDal.getInstance()
							.createCriteria(shppev_rIdentification.class);
					queryReason.add(Restrictions.eq(shppev_rIdentification.PROPERTY_REASON, auxValidate));
					List<shppev_rIdentification> listReason = queryReason.list();
					if (listReason.size() > 0) {
						shppev_rIdentification objReason = listReason.get(0);
						persona.put("msgLN", objReason.getMessage());
						persona.put("idLN", ID);
						persona.put("matrizReason", "Motivo cédula");
						if (objReason.getShppwsAnswer().equals("C")) {
							validate_CI = true;
						} else {
							validate_CI = false;
							break;
						}
					} else {
						validate_CI = true;//C if no exist in blacklist
					}

				} catch (Exception e) {
					persona.put("msgLN", "msg erroneo en motivo cédula");
					persona.put("idLN", ID);
					persona.put("matrizReason", "Motivo cédula");
					validate_CI = false;
					break;
				}
			}
		}
		return validate_CI;
	}
	
	public boolean validateLN_Cellphone( String ID, String apiResponse, Map<String, Object> persona)throws JSONException {
		JSONArray arrayBlacklist = new JSONArray(apiResponse);
		Boolean validate_Celphone = true;
		
		for (int i = 0; i < arrayBlacklist.length(); i++) {
			JSONObject objBlacklist = arrayBlacklist.getJSONObject(i);
			String auxValidate = objBlacklist.getString("tipo");
			if (auxValidate.equals("No existe registro.") || auxValidate.equals("ERROR KEY")) {
				validate_Celphone = true;
			} else {
				try {
					OBCriteria<shppev_phoneReason> queryReason = OBDal.getInstance()
							.createCriteria(shppev_phoneReason.class);
					queryReason.add(Restrictions.eq(shppev_phoneReason.PROPERTY_REASON, auxValidate));
					List<shppev_phoneReason> listReason = queryReason.list();
					if (listReason.size() > 0) {
						shppev_phoneReason objReason = listReason.get(0);
						persona.put("msgLN", objReason.getMessage());
						persona.put("idLN", ID);
						persona.put("matrizReason", "Motivo teléfono");
						if (objReason.getShppwsAnswer().equals("C")) {
							validate_Celphone = true;
						} else {
							validate_Celphone = false;
							break;
						}
					} else {
						validate_Celphone = true;
					}
				} catch (Exception e) {
					persona.put("msgLN", "msg erroneo en motivo teléfono");
					persona.put("idLN", ID);
					persona.put("matrizReason", "Motivo teléfono");
					validate_Celphone = false;
					break;
				}
			}
		}
		return validate_Celphone;
	}
	
	public boolean validateLN_Email( String ID, String apiResponse, Map<String, Object> persona)throws JSONException {
		JSONArray arrayBlacklist = new JSONArray(apiResponse);
		Boolean validate_Email = true;
		
		for (int i = 0; i < arrayBlacklist.length(); i++) {
			JSONObject objBlacklist = arrayBlacklist.getJSONObject(i);
			String auxValidate = objBlacklist.getString("tipo");
			if (auxValidate.equals("No existe registro.") || auxValidate.equals("ERROR KEY")) {
				validate_Email = true;
			} else {
				try {
					OBCriteria<shppev_emailReason> queryReason = OBDal.getInstance()
							.createCriteria(shppev_emailReason.class);
					queryReason.add(Restrictions.eq(shppev_emailReason.PROPERTY_REASON, auxValidate));
					List<shppev_emailReason> listReason = queryReason.list();
					if (listReason.size() > 0) {
						shppev_emailReason objReason = listReason.get(0);
						persona.put("msgLN", objReason.getMessage());
						persona.put("idLN", ID);
						persona.put("matrizReason", "Motivo correo");
						if (objReason.getShppwsAnswer().equals("C")) {
							validate_Email = true;
						} else {
							validate_Email = false;
							break;
						}
					} else {
						validate_Email = true;
					}
				} catch (Exception e) {
					persona.put("msgLN", "msg erroneo en motivo correo");
					persona.put("idLN", ID);
					persona.put("matrizReason", "Motivo correo");
					validate_Email = false;
					break;
				}
			}
		}
		return validate_Email;
	}
	
	public boolean validateLN_Profession( Map<String, Object> persona)throws JSONException {
			String RCprofession = (String) persona.get("RCprofession");
			OBCriteria<shppev_reasonProfession> queryProfession= OBDal.getInstance().createCriteria(shppev_reasonProfession.class);
	  		queryProfession.add(Restrictions.eq(shppev_reasonProfession.PROPERTY_PROFESSION, RCprofession));
			List<shppev_reasonProfession> listProfessions = queryProfession.list();
			int sizeprofession=listProfessions.size();
			if(sizeprofession > 0) {
				shppev_reasonProfession objReason = listProfessions.get(0);
				persona.put("msgLN", objReason.getMessage());
		    	persona.put("idLN",RCprofession);
		    	persona.put("matrizReason","Motivo Profesión");
				return false;
			}else {
				return true;
			}
	}
	
	public boolean validateDataDI( String apiResponse, Map<String, Object> persona)throws JSONException {//Deuda interna
		JSONArray arrayBlacklist = new JSONArray(apiResponse);
		Boolean validate_DI = true;
		Boolean validateDefault = true;
		for (int i = 0; i < arrayBlacklist.length(); i++) {
			JSONObject objInternalDue = arrayBlacklist.getJSONObject(i);
			String auxValidate = objInternalDue.getString("cedente");
			BigDecimal auxAmmount = new BigDecimal(objInternalDue.getString("total_Pagar"));
			Long auxDueDays = new Long(objInternalDue.getString("dias_Mora"));
			if (auxAmmount == null || auxAmmount.compareTo(BigDecimal.ZERO) < 0) {auxAmmount = BigDecimal.ZERO;}
			if (auxDueDays == null || auxDueDays < 0) {auxDueDays = new Long(0);}
			persona.put("recover_days_late", auxDueDays);
			persona.put("recover_amount_pay", auxAmmount);
			if (auxValidate.equals("No existe credito.") || auxValidate.equals("ERROR KEY")) {
				validate_DI = true;
			} else {
				try {
					OBCriteria<shppev_internalDebt> queryReason = OBDal.getInstance().createCriteria(shppev_internalDebt.class);
					queryReason.add(Restrictions.eq(shppev_internalDebt.PROPERTY_CREDITOR, auxValidate));
					List<shppev_internalDebt> listReason = queryReason.list();
					if (listReason.size() > 0) {
						for (shppev_internalDebt objReason : listReason) {
							if (auxAmmount.compareTo(objReason.getLowerrangemount()) >= 0
									&& auxAmmount.compareTo(objReason.getUpperrangemount()) <= 0) {
								if ((auxDueDays >= objReason.getLowerrangedays())&& (auxDueDays <= objReason.getUpperrangedays())) {
									validateDefault = false;
									String response = objReason.getAnswer();
									if (response.equals("C")) {
										validate_DI = true;
									} else {
										persona.put("msgLN", objReason.getMessage());
										persona.put("idLN", auxValidate + ", " + auxAmmount + ", " + auxDueDays);
										persona.put("matrizReason", "Deuda Interna");
										validate_DI = false;
										break;
									}
								}
							}
						}
					}
					
					//DEFAULT
					if (validateDefault) {
						OBCriteria<shppev_internalDebt> queryDefault = OBDal.getInstance()
								.createCriteria(shppev_internalDebt.class);
						List<shppev_internalDebt> listyDefault = queryDefault.list();
						for (shppev_internalDebt deafultObj : listyDefault) {
							if (deafultObj.isShppwsDefaultField()) {
								String response = deafultObj.getAnswer();
								if (response.equals("C")) {
									validate_DI = true;
								} else {
									persona.put("matrizReason", "Deuda Interna");
									persona.put("idLN", auxValidate + ", " + auxAmmount + ", " + auxDueDays);
									persona.put("msgLN", deafultObj.getShppwsDefaultMessage() + "");
									persona.put("filterRC", deafultObj.getAnswer());
									validate_DI = false;
									break;
								}
							}
						}
					}
					
				} catch (Exception e) {
					persona.put("msgLN", "msg erroneo en servicio Deuda Interna");
					persona.put("idLN", auxValidate + ", " + auxAmmount + ", " + auxDueDays);
					persona.put("matrizReason", "Deuda Interna");
					validate_DI = false;
					break;
				}
			}
		}
		return validate_DI;
		
    }

	 public String validateStatusPartner(String apiResponse) throws JSONException {
		 JSONObject jsonResponse = new JSONObject(apiResponse);
	     String respuestaApi = jsonResponse.getString("clasificacion_cliente");
			String TipoClienteAprobado="";
			OBCriteria<shppwsPartnertype> queryPartnerType= OBDal.getInstance().createCriteria(shppwsPartnertype.class);
			queryPartnerType.add(Restrictions.eq(shppwsPartnertype.PROPERTY_SEARCHKEY, respuestaApi));
			shppwsPartnertype accesPartnerType = (shppwsPartnertype) queryPartnerType.uniqueResult();
			List<shppwsPartnertype> listobjPartnerList = queryPartnerType.list();
			if(listobjPartnerList.size() > 0) {
				TipoClienteAprobado=accesPartnerType.getCommercialName();
			}else{
				TipoClienteAprobado="CLIENTE RECHAZADO";//CLIENTE RECHAZADO
			}
			return TipoClienteAprobado;
		}
	 
	 public String classificationClient(String apiResponse) throws JSONException {
		 JSONObject jsonResponse = new JSONObject(apiResponse);
	     String respuestaApi = jsonResponse.getString("clasificacion_cliente");
			return respuestaApi;
		}
	 
	 public boolean customerNewOld(Map<String, Object> persona, BusinessPartner partner)throws JSONException  {
			String identifier = (String) persona.get("Identifier");
			OBCriteria<Invoice> queryInvoices = OBDal.getInstance().createCriteria(Invoice.class);
			queryInvoices.add(Restrictions.eq(Invoice.PROPERTY_BUSINESSPARTNER,partner));
			queryInvoices.add(Restrictions.eq(Invoice.PROPERTY_DOCUMENTSTATUS,"CO"));
			List<Invoice> listobjInvoice = queryInvoices.list();
			if(listobjInvoice.size() > 0) {
				return false;
			}
			return true;
		}
	 
	 public boolean customerOldCC_uncheck(BusinessPartner partner, Map<String, Object> persona)throws JSONException  {
			Long ccPartner;
			try {
				 ccPartner = partner.getShpctNoPunishedCredits();
				 if(ccPartner == null) {
						ccPartner = new Long(0);
				  }
			}catch(Exception e) { ccPartner = new Long(0); }
			
			
			
			OBCriteria<Shppec_CredPen> queryCredPen = OBDal.getInstance().createCriteria(Shppec_CredPen.class);
			List<Shppec_CredPen> listobjCredPen = queryCredPen.list();
			
			if(listobjCredPen.size() > 0) {
				for(Shppec_CredPen objCredPen : listobjCredPen) {
					String validatorLogic = objCredPen.getCompare();
					BigDecimal validatorValue = objCredPen.getCredPenal();
					Long newvalidatorValue = validatorValue.longValue();
					if(validatorLogic.equals("<=")) {
						if(ccPartner <= newvalidatorValue ) {
							String response = objCredPen.getOutput();
	    					if(response.equals("R")) {
	    						persona.put("msgLN", objCredPen.getMessage());
	    				    	persona.put("idLN",ccPartner.toString());
	    				    	persona.put("matrizReason","Matriz Crédito Castigado");
	    				    	return false;
	    					}else {
	    						return true;
	    					}
						}
					}else if(validatorLogic.equals(">=")) {
						if(ccPartner >= newvalidatorValue ) {
							String response = objCredPen.getOutput();
	    					if(response.equals("R")) {
	    						persona.put("msgLN", objCredPen.getMessage());
	    				    	persona.put("idLN",ccPartner.toString());
	    				    	persona.put("matrizReason","Matriz Crédito Castigado");
	    				    	return false;
	    					}else {
	    						return true;
	    					}
						}
					}else if(validatorLogic.equals(">")) {
						if(ccPartner > newvalidatorValue ) {
							String response = objCredPen.getOutput();
	    					if(response.equals("R")) {
	    						persona.put("msgLN", objCredPen.getMessage());
	    				    	persona.put("idLN",ccPartner.toString());
	    				    	persona.put("matrizReason","Matriz Crédito Castigado");
	    				    	return false;
	    					}else {
	    						return true;
	    					}
						}
					}else if(validatorLogic.equals("<")) {
						if(ccPartner < newvalidatorValue ) {
							String response = objCredPen.getOutput();
	    					if(response.equals("R")) {
	    						persona.put("msgLN", objCredPen.getMessage());
	    				    	persona.put("idLN",ccPartner.toString());
	    				    	persona.put("matrizReason","Matriz Crédito Castigado");
	    				    	return false;
	    					}else {
	    						return true;
	    					}
						}
					}else if(validatorLogic.equals("=")) {
						if(ccPartner.equals(newvalidatorValue) ) {
							String response = objCredPen.getOutput();
	    					if(response.equals("R")) {
	    						persona.put("msgLN", objCredPen.getMessage());
	    				    	persona.put("idLN",ccPartner.toString());
	    				    	persona.put("matrizReason","Matriz Crédito Castigado");
	    				    	return false;
	    					}else {
	    						return true;
	    					}
						}
					}
				}
			}
			
			// DEFAULT
			for (Shppec_CredPen deafultObj : listobjCredPen) {
				if (deafultObj.isShppwsDefaultField()) {
					String response = deafultObj.getOutput();
					if (response.equals("R")) {
						persona.put("matrizReason", "Matriz Crédito Castigado");
						persona.put("idLN", ccPartner.toString() + "");
						persona.put("msgLN", deafultObj.getShppwsDefaultMessage() + "");
						return false;
					} else {
						return true;
					}
				}
			}
	    	
			
			persona.put("msgLN", "Registro no encontrado");
	    	persona.put("idLN",ccPartner.toString()+"");
	    	persona.put("matrizReason","Matriz Crédito Castigado");
			return false;
		}
		
		public boolean customerOldCV_uncheck(BusinessPartner partner, Map<String, Object> persona)throws JSONException  {
			Long cvPartner;
			try {
				cvPartner = partner.getShpctNoCurrentCredits();
				 if(cvPartner == null) {
					 cvPartner = new Long(0);
				  }
			}catch(Exception e) { cvPartner = new Long(0); }
			
			
			OBCriteria<Shppec_CredCurr> queryCredCurr = OBDal.getInstance().createCriteria(Shppec_CredCurr.class);
			List<Shppec_CredCurr> listobjCredCurr = queryCredCurr.list();
			if(listobjCredCurr.size() > 0) {
				for(Shppec_CredCurr objCredV : listobjCredCurr) {
					String validatorLogic = objCredV.getCompare();
					BigDecimal validatorValue = objCredV.getCredCurrent();
					Long newvalidatorValue = validatorValue.longValue();
					if(validatorLogic.equals("<=")) {
						if(cvPartner <= newvalidatorValue ) {
							String response = objCredV.getResult();
	    					if(response.equals("R")) {
	    						persona.put("msgLN", objCredV.getMessage());
	    				    	persona.put("idLN",cvPartner.toString());
	    				    	persona.put("matrizReason","Matriz Crédito Vigente");
	    				    	return false;
	    					}else {
	    						return true;
	    					}
						}
					}else if(validatorLogic.equals(">=")) {
						if(cvPartner >= newvalidatorValue ) {
							String response = objCredV.getResult();
	    					if(response.equals("R")) {
	    						persona.put("msgLN", objCredV.getMessage());
	    				    	persona.put("idLN",cvPartner.toString());
	    				    	persona.put("matrizReason","Matriz Crédito Vigente");
	    				    	return false;
	    					}else {
	    						return true;
	    					}
						}
					}else if(validatorLogic.equals(">")) {
						if(cvPartner > newvalidatorValue ) {
							String response = objCredV.getResult();
	    					if(response.equals("R")) {
	    						persona.put("msgLN", objCredV.getMessage());
	    				    	persona.put("idLN",cvPartner.toString());
	    				    	persona.put("matrizReason","Matriz Crédito Vigente");
	    				    	return false;
	    					}else {
	    						return true;
	    					}
						}
					}else if(validatorLogic.equals("<")) {
						if(cvPartner < newvalidatorValue ) {
							String response = objCredV.getResult();
	    					if(response.equals("R")) {
	    						persona.put("msgLN", objCredV.getMessage());
	    				    	persona.put("idLN",cvPartner.toString());
	    				    	persona.put("matrizReason","Matriz Crédito Vigente");
	    				    	return false;
	    					}else {
	    						return true;
	    					}
						}
					}else if(validatorLogic.equals("=")) {
						if(cvPartner.equals(newvalidatorValue)) {
							String response = objCredV.getResult();
	    					if(response.equals("R")) {
	    						persona.put("msgLN", objCredV.getMessage());
	    				    	persona.put("idLN",cvPartner.toString());
	    				    	persona.put("matrizReason","Matriz Crédito Vigente");
	    				    	return false;
	    					}else {
	    						return true;
	    					}
						}
					}
				}
			}
			
			// DEFAULT
			for (Shppec_CredCurr deafultObj : listobjCredCurr) {
				if (deafultObj.isShppwsDefaultField()) {
					String response = deafultObj.getResult();
					if (response.equals("R")) {
						persona.put("matrizReason", "Matriz Crédito Vigente");
						persona.put("idLN", cvPartner.toString() + "");
						persona.put("msgLN", deafultObj.getShppwsDefaultMessage() + "");
						return false;
					} else {
						return true;
					}
				}
			}
					
			persona.put("msgLN", "Registro no encontrado");
	    	persona.put("idLN",cvPartner.toString()+"");
	    	persona.put("matrizReason","Matriz Crédito Vigente");
			return false;
		}
		
		public boolean customerOldCVV_uncheck(BusinessPartner partner, Map<String, Object> persona)throws JSONException  {
			Long cvvPartner;
			try {
				cvvPartner = partner.getShpctNoCCreditsExpired();
				 if(cvvPartner == null) {
					 cvvPartner = new Long(0);
				  }
			}catch(Exception e) { cvvPartner = new Long(0); }
			
			
			OBCriteria<Shppec_CredExp> queryCredExp = OBDal.getInstance().createCriteria(Shppec_CredExp.class);
			List<Shppec_CredExp> listobjCredExp = queryCredExp.list();
			if(listobjCredExp.size() > 0) {
				for(Shppec_CredExp objCredVV : listobjCredExp) {
					String validatorLogic = objCredVV.getCompare();
					BigDecimal validatorValue = objCredVV.getCredExpired();
					Long newvalidatorValue = validatorValue.longValue();
					if(validatorLogic.equals("<=")) {
						if(cvvPartner <= newvalidatorValue ) {
							String response = objCredVV.getResult();
	    					if(response.equals("R")) {
	    						persona.put("msgLN", objCredVV.getMessage());
	    				    	persona.put("idLN",cvvPartner.toString());
	    				    	persona.put("matrizReason","Matriz Crédito Vigente Vencido");
	    				    	return false;
	    					}else {
	    						return true;
	    					}
						}
					}else if(validatorLogic.equals(">=")) {
						if(cvvPartner >= newvalidatorValue ) {
							String response = objCredVV.getResult();
	    					if(response.equals("R")) {
	    						persona.put("msgLN", objCredVV.getMessage());
	    				    	persona.put("idLN",cvvPartner.toString());
	    				    	persona.put("matrizReason","Matriz Crédito Vigente Vencido");
	    				    	return false;
	    					}else {
	    						return true;
	    					}
						}
					}else if(validatorLogic.equals(">")) {
						if(cvvPartner > newvalidatorValue ) {
							String response = objCredVV.getResult();
	    					if(response.equals("R")) {
	    						persona.put("msgLN", objCredVV.getMessage());
	    				    	persona.put("idLN",cvvPartner.toString());
	    				    	persona.put("matrizReason","Matriz Crédito Vigente Vencido");
	    				    	return false;
	    					}else {
	    						return true;
	    					}
						}
					}else if(validatorLogic.equals("<")) {
						if(cvvPartner < newvalidatorValue ) {
							String response = objCredVV.getResult();
	    					if(response.equals("R")) {
	    						persona.put("msgLN", objCredVV.getMessage());
	    				    	persona.put("idLN",cvvPartner.toString());
	    				    	persona.put("matrizReason","Matriz Crédito Vigente Vencido");
	    				    	return false;
	    					}else {
	    						return true;
	    					}
						}
					}else if(validatorLogic.equals("=")) {
						if(cvvPartner.equals(newvalidatorValue) ) {
							String response = objCredVV.getResult();
	    					if(response.equals("R")) {
	    						persona.put("msgLN", objCredVV.getMessage());
	    				    	persona.put("idLN",cvvPartner.toString());
	    				    	persona.put("matrizReason","Matriz Crédito Vigente Vencido");
	    				    	return false;
	    					}else {
	    						return true;
	    					}
						}
					}
				}
			}
			
			// DEFAULT
			for (Shppec_CredExp deafultObj :  listobjCredExp) {
				if (deafultObj.isShppwsDefaultField()) {
					String response = deafultObj.getResult();
					if (response.equals("R")) {
						persona.put("matrizReason", "Matriz Crédito Vigente Vencido");
						persona.put("idLN", cvvPartner.toString() + "");
						persona.put("msgLN", deafultObj.getShppwsDefaultMessage() + "");
						return false;
					} else {
						return true;
					}
				}
			}
			
			persona.put("msgLN", "Registro no encontrado");
	    	persona.put("idLN",cvvPartner.toString()+"");
	    	persona.put("matrizReason","Matriz Crédito Vigente Vencido");
			return false;
		}
	 
	 public String SearchClieSegm(String apiResponse) throws JSONException {
		 JSONObject jsonResponse = new JSONObject(apiResponse);
	        JSONObject comportamientoCrediticio = jsonResponse.getJSONObject("data").getJSONObject("comportamiento_crediticio");
	        String clieSegmento = comportamientoCrediticio.getString("CLIE_PERFIL_COMP_PAGO_INTERNO"); 
			return clieSegmento;
		}
	
	public boolean customerOldCC(BusinessPartner partner, Map<String, Object> persona, String typePartner)throws JSONException  {
		Long ccPartner;
		try {
			 ccPartner = partner.getShpctNoPunishedCredits();
			 if(ccPartner == null) {
					ccPartner = new Long(0);
			  }
		}catch(Exception e) { ccPartner = new Long(0); }
		
		
		if(typePartner.equals("CLIENTE NUEVO")||typePartner.equals("CLIENTE APROBADO")) {
		OBCriteria<Shppec_CredPen> queryCredPen = OBDal.getInstance().createCriteria(Shppec_CredPen.class);
		List<Shppec_CredPen> listobjCredPen = queryCredPen.list();
		
		if(listobjCredPen.size() > 0) {
			for(Shppec_CredPen objCredPen : listobjCredPen) {
				String validatorLogic = objCredPen.getCompare();
				BigDecimal validatorValue = objCredPen.getCredPenal();
				Long newvalidatorValue = validatorValue.longValue();
				if(validatorLogic.equals("<=")) {
					if(ccPartner <= newvalidatorValue ) {
						String response = objCredPen.getOutput();
    					if(response.equals("R")) {
    						persona.put("msgLN", objCredPen.getMessage());
    				    	persona.put("idLN",ccPartner.toString());
    				    	persona.put("matrizReason","Matriz Crédito Castigado");
    				    	return false;
    					}else {
    						return true;
    					}
					}
				}else if(validatorLogic.equals(">=")) {
					if(ccPartner >= newvalidatorValue ) {
						String response = objCredPen.getOutput();
    					if(response.equals("R")) {
    						persona.put("msgLN", objCredPen.getMessage());
    				    	persona.put("idLN",ccPartner.toString());
    				    	persona.put("matrizReason","Matriz Crédito Castigado");
    				    	return false;
    					}else {
    						return true;
    					}
					}
				}else if(validatorLogic.equals(">")) {
					if(ccPartner > newvalidatorValue ) {
						String response = objCredPen.getOutput();
    					if(response.equals("R")) {
    						persona.put("msgLN", objCredPen.getMessage());
    				    	persona.put("idLN",ccPartner.toString());
    				    	persona.put("matrizReason","Matriz Crédito Castigado");
    				    	return false;
    					}else {
    						return true;
    					}
					}
				}else if(validatorLogic.equals("<")) {
					if(ccPartner < newvalidatorValue ) {
						String response = objCredPen.getOutput();
    					if(response.equals("R")) {
    						persona.put("msgLN", objCredPen.getMessage());
    				    	persona.put("idLN",ccPartner.toString());
    				    	persona.put("matrizReason","Matriz Crédito Castigado");
    				    	return false;
    					}else {
    						return true;
    					}
					}
				}else if(validatorLogic.equals("=")) {
					if(ccPartner.equals(newvalidatorValue) ) {
						String response = objCredPen.getOutput();
    					if(response.equals("R")) {
    						persona.put("msgLN", objCredPen.getMessage());
    				    	persona.put("idLN",ccPartner.toString());
    				    	persona.put("matrizReason","Matriz Crédito Castigado");
    				    	return false;
    					}else {
    						return true;
    					}
					}
				}
			}
		}
		
		// DEFAULT
		for (Shppec_CredPen deafultObj : listobjCredPen) {
			if (deafultObj.isShppwsDefaultField()) {
				String response = deafultObj.getOutput();
				if (response.equals("R")) {
					persona.put("matrizReason", "Matriz Crédito Castigado");
					persona.put("idLN", ccPartner.toString() + "");
					persona.put("msgLN", deafultObj.getShppwsDefaultMessage() + "");
					return false;
				} else {
					return true;
				}
			}
		}
    	
		
		persona.put("msgLN", "Registro no encontrado");
    	persona.put("idLN",ccPartner.toString()+"");
    	persona.put("matrizReason","Matriz Crédito Castigado");
		return false;
		}else {
			OBCriteria<shppee_Quotas> qDefaultMsg = OBDal.getInstance().createCriteria(shppee_Quotas.class);
		      qDefaultMsg.add(Restrictions.eq(shppee_Quotas.PROPERTY_SHPPWSDEFAULTFIELD, true));
		      qDefaultMsg.setMaxResults(1);			
		      shppee_Quotas msgDefault = (shppee_Quotas) qDefaultMsg.uniqueResult();
			persona.put("msgLN", msgDefault.getShppwsDefaultMessage());
	    	persona.put("idLN",ccPartner.toString()+"");
	    	persona.put("matrizReason","Matriz Crédito Vigente Vencido");
			return false;
		}
	}
	
	public boolean customerOldCV(BusinessPartner partner, Map<String, Object> persona, String typePartner)throws JSONException  {
		Long cvPartner;
		try {
			cvPartner = partner.getShpctNoCurrentCredits();
			 if(cvPartner == null) {
				 cvPartner = new Long(0);
			  }
		}catch(Exception e) { cvPartner = new Long(0); }
		
		if(typePartner.equals("CLIENTE NUEVO")||typePartner.equals("CLIENTE APROBADO")) {
		OBCriteria<Shppec_CredCurr> queryCredCurr = OBDal.getInstance().createCriteria(Shppec_CredCurr.class);
		List<Shppec_CredCurr> listobjCredCurr = queryCredCurr.list();
		if(listobjCredCurr.size() > 0) {
			for(Shppec_CredCurr objCredV : listobjCredCurr) {
				String validatorLogic = objCredV.getCompare();
				BigDecimal validatorValue = objCredV.getCredCurrent();
				Long newvalidatorValue = validatorValue.longValue();
				if(validatorLogic.equals("<=")) {
					if(cvPartner <= newvalidatorValue ) {
						String response = objCredV.getResult();
    					if(response.equals("R")) {
    						persona.put("msgLN", objCredV.getMessage());
    				    	persona.put("idLN",cvPartner.toString());
    				    	persona.put("matrizReason","Matriz Crédito Vigente");
    				    	return false;
    					}else {
    						return true;
    					}
					}
				}else if(validatorLogic.equals(">=")) {
					if(cvPartner >= newvalidatorValue ) {
						String response = objCredV.getResult();
    					if(response.equals("R")) {
    						persona.put("msgLN", objCredV.getMessage());
    				    	persona.put("idLN",cvPartner.toString());
    				    	persona.put("matrizReason","Matriz Crédito Vigente");
    				    	return false;
    					}else {
    						return true;
    					}
					}
				}else if(validatorLogic.equals(">")) {
					if(cvPartner > newvalidatorValue ) {
						String response = objCredV.getResult();
    					if(response.equals("R")) {
    						persona.put("msgLN", objCredV.getMessage());
    				    	persona.put("idLN",cvPartner.toString());
    				    	persona.put("matrizReason","Matriz Crédito Vigente");
    				    	return false;
    					}else {
    						return true;
    					}
					}
				}else if(validatorLogic.equals("<")) {
					if(cvPartner < newvalidatorValue ) {
						String response = objCredV.getResult();
    					if(response.equals("R")) {
    						persona.put("msgLN", objCredV.getMessage());
    				    	persona.put("idLN",cvPartner.toString());
    				    	persona.put("matrizReason","Matriz Crédito Vigente");
    				    	return false;
    					}else {
    						return true;
    					}
					}
				}else if(validatorLogic.equals("=")) {
					if(cvPartner.equals(newvalidatorValue)) {
						String response = objCredV.getResult();
    					if(response.equals("R")) {
    						persona.put("msgLN", objCredV.getMessage());
    				    	persona.put("idLN",cvPartner.toString());
    				    	persona.put("matrizReason","Matriz Crédito Vigente");
    				    	return false;
    					}else {
    						return true;
    					}
					}
				}
			}
		}
		
		// DEFAULT
		for (Shppec_CredCurr deafultObj : listobjCredCurr) {
			if (deafultObj.isShppwsDefaultField()) {
				String response = deafultObj.getResult();
				if (response.equals("R")) {
					persona.put("matrizReason", "Matriz Crédito Vigente");
					persona.put("idLN", cvPartner.toString() + "");
					persona.put("msgLN", deafultObj.getShppwsDefaultMessage() + "");
					return false;
				} else {
					return true;
				}
			}
		}
				
		persona.put("msgLN", "Registro no encontrado");
    	persona.put("idLN",cvPartner.toString()+"");
    	persona.put("matrizReason","Matriz Crédito Vigente");
		return false;
	}else {
			persona.put("msgLN", "Registro no encontrado");
	    	persona.put("idLN",cvPartner.toString()+"");
	    	persona.put("matrizReason","Matriz Crédito Vigente Vencido");
			return false;
		}
	}
	
	
	public boolean customerOldCVV(BusinessPartner partner, Map<String, Object> persona, String typePartner)throws JSONException  {
		
			
		
		Long cvvPartner;
		try {
			cvvPartner = partner.getShpctNoCCreditsExpired();
			 if(cvvPartner == null) {
				 cvvPartner = new Long(0);
			  }
		}catch(Exception e) { cvvPartner = new Long(0); }
		
		if(typePartner.equals("CLIENTE NUEVO")||typePartner.equals("CLIENTE APROBADO")) {
		OBCriteria<Shppec_CredExp> queryCredExp = OBDal.getInstance().createCriteria(Shppec_CredExp.class);
		List<Shppec_CredExp> listobjCredExp = queryCredExp.list();
		if(listobjCredExp.size() > 0) {
			for(Shppec_CredExp objCredVV : listobjCredExp) {
				String validatorLogic = objCredVV.getCompare();
				BigDecimal validatorValue = objCredVV.getCredExpired();
				Long newvalidatorValue = validatorValue.longValue();
				if(validatorLogic.equals("<=")) {
					if(cvvPartner <= newvalidatorValue ) {
						String response = objCredVV.getResult();
    					if(response.equals("R")) {
    						persona.put("msgLN", objCredVV.getMessage());
    				    	persona.put("idLN",cvvPartner.toString());
    				    	persona.put("matrizReason","Matriz Crédito Vigente Vencido");
    				    	return false;
    					}else {
    						return true;
    					}
					}
				}else if(validatorLogic.equals(">=")) {
					if(cvvPartner >= newvalidatorValue ) {
						String response = objCredVV.getResult();
    					if(response.equals("R")) {
    						persona.put("msgLN", objCredVV.getMessage());
    				    	persona.put("idLN",cvvPartner.toString());
    				    	persona.put("matrizReason","Matriz Crédito Vigente Vencido");
    				    	return false;
    					}else {
    						return true;
    					}
					}
				}else if(validatorLogic.equals(">")) {
					if(cvvPartner > newvalidatorValue ) {
						String response = objCredVV.getResult();
    					if(response.equals("R")) {
    						persona.put("msgLN", objCredVV.getMessage());
    				    	persona.put("idLN",cvvPartner.toString());
    				    	persona.put("matrizReason","Matriz Crédito Vigente Vencido");
    				    	return false;
    					}else {
    						return true;
    					}
					}
				}else if(validatorLogic.equals("<")) {
					if(cvvPartner < newvalidatorValue ) {
						String response = objCredVV.getResult();
    					if(response.equals("R")) {
    						persona.put("msgLN", objCredVV.getMessage());
    				    	persona.put("idLN",cvvPartner.toString());
    				    	persona.put("matrizReason","Matriz Crédito Vigente Vencido");
    				    	return false;
    					}else {
    						return true;
    					}
					}
				}else if(validatorLogic.equals("=")) {
					if(cvvPartner.equals(newvalidatorValue) ) {
						String response = objCredVV.getResult();
    					if(response.equals("R")) {
    						persona.put("msgLN", objCredVV.getMessage());
    				    	persona.put("idLN",cvvPartner.toString());
    				    	persona.put("matrizReason","Matriz Crédito Vigente Vencido");
    				    	return false;
    					}else {
    						return true;
    					}
					}
				}
			}
		}
		
		// DEFAULT
		for (Shppec_CredExp deafultObj :  listobjCredExp) {
			if (deafultObj.isShppwsDefaultField()) {
				String response = deafultObj.getResult();
				if (response.equals("R")) {
					persona.put("matrizReason", "Matriz Crédito Vigente Vencido");
					persona.put("idLN", cvvPartner.toString() + "");
					persona.put("msgLN", deafultObj.getShppwsDefaultMessage() + "");
					return false;
				} else {
					return true;
				}
			}
		}
		
		persona.put("msgLN", "Registro no encontrado");
    	persona.put("idLN",cvvPartner.toString()+"");
    	persona.put("matrizReason","Matriz Crédito Vigente Vencido");
		return false;
		}else {
			persona.put("msgLN", "Registro no encontrado");
	    	persona.put("idLN",cvvPartner.toString()+"");
	    	persona.put("matrizReason","Matriz Crédito Vigente Vencido");
			return false;
		}
	}
	
	public String parallelCredits(BusinessPartner partner, Map<String, Object> persona)throws JSONException  {
		Long mfpaid=partner.getShpctMaximumFeePaid(); //N0 plazo
		Long lipaid=partner.getShpctLastInstallmentpaid();//Última cuota
		
			 if(lipaid == null) {lipaid = new Long(0);}
			 if(mfpaid == null) {mfpaid = new Long(0);}
		
		OBCriteria<Shppec_ParallelC> queryParallelC = OBDal.getInstance().createCriteria(Shppec_ParallelC.class);
		List<Shppec_ParallelC> listObjParallelC = queryParallelC.list();
		if(listObjParallelC.size() > 0) {
			for(Shppec_ParallelC objParallelC : listObjParallelC) {
				String validatorLogic = objParallelC.getComparison();
				Long validatorTerm = objParallelC.getTerm();
				Long validatorQuota = objParallelC.getMINQuote();
				if(validatorLogic.equals("<=")) {
					if(mfpaid.equals(validatorTerm) && lipaid <= validatorQuota ) {
						String response = objParallelC.getResponse();
						return response;
					}
				}else if(validatorLogic.equals(">=")) {
					if(mfpaid.equals(validatorTerm) && lipaid >= validatorQuota ) {
						String response = objParallelC.getResponse();
						return response;
					}
				}else if(validatorLogic.equals("<")) {
					if(mfpaid.equals(validatorTerm) && lipaid < validatorQuota ) {
						String response = objParallelC.getResponse();
						return response;
					}
				}else if(validatorLogic.equals(">")) {
					if(mfpaid.equals(validatorTerm) && lipaid > validatorQuota ) {
						String response = objParallelC.getResponse();
						return response;
					}
				}else if(validatorLogic.equals("=")) {
					if(mfpaid.equals(validatorTerm) && lipaid.equals(validatorQuota) ) {
						String response = objParallelC.getResponse();
						return response;
					}
				}
			}
		}
		
		//DEFAULT X
		for (Shppec_ParallelC deafultObj : listObjParallelC) {
			if (deafultObj.isShppwsDefaultField()) {
				String response = deafultObj.getResponse();
				return response;
			}
		}
		
		return "F";
	}
	
	public String customerRiskIndex(BusinessPartner partner,String VoF, Map<String, Object> persona)throws JSONException  {
		
		Long ncpaid=partner.getShpctNoCreditsPaid(); //No. Créditos Pagados
		BigDecimal irisk=partner.getShpctRiskIndex();//Índice de Riesgo
		String segmentationEQ = (String)persona.get("EQ_segmentacion");
		
			 if(ncpaid == null) {ncpaid = new Long(0);}
			 if(irisk == null) {irisk = new BigDecimal(0);}
			 Long newvalueRiskIndex=irisk.longValue();	 
		
		OBCriteria<shppee_RiskIndex> queryRiskIndex= OBDal.getInstance().createCriteria(shppee_RiskIndex.class);
		queryRiskIndex.add(Restrictions.eq(shppee_RiskIndex.PROPERTY_ANSWER,VoF));
		List<shppee_RiskIndex> listObjRiskIndex = queryRiskIndex.list();
		if(listObjRiskIndex.size() > 0) {
			for(shppee_RiskIndex objParallelC : listObjRiskIndex) {
				String EQsegments = objParallelC.getProfileCampPayment();
				Long credcan = new Long (objParallelC.getCredCanc());
				Long from = new Long (objParallelC.getFrom());
				Long until = new Long (objParallelC.getUntil());
				//SI SE MANTIENE CON .contains() ABARCA VACIOS
				if (segmentationEQ != null && !segmentationEQ.isEmpty() && EQsegments.contains(segmentationEQ) && (newvalueRiskIndex >= from && newvalueRiskIndex <= until)) {
					String validatorLogic = objParallelC.getCompare();
					if(validatorLogic.equals("<=")) {
						if(ncpaid <= credcan) {
							String response = objParallelC.getSegment();
							if(response.equals("R")) {
								persona.put("msgLN", objParallelC.getMessage());
	    				    	persona.put("idLN","Riesgo:"+irisk+", "+" Créditos pagados:"+ncpaid);
	    				    	persona.put("matrizReason","Matriz Índice de Riesgo");
							}
							return response;
						}
					}else if(validatorLogic.equals(">=")) {
						if(ncpaid >= credcan) {
							String response = objParallelC.getSegment();
							if(response.equals("R")) {
								persona.put("msgLN", objParallelC.getMessage());
	    				    	persona.put("idLN","Riesgo:"+irisk+", "+" Créditos pagados:"+ncpaid);
	    				    	persona.put("matrizReason","Matriz Índice de Riesgo");
							}
							return response;
						}
					}else if(validatorLogic.equals("<")) {
						if(ncpaid < credcan) {
							String response = objParallelC.getSegment();
							if(response.equals("R")) {
								persona.put("msgLN", objParallelC.getMessage());
	    				    	persona.put("idLN","Riesgo:"+irisk+", "+" Créditos pagados:"+ncpaid);
	    				    	persona.put("matrizReason","Matriz Índice de Riesgo");
							}
							return response;
						}
					}else if(validatorLogic.equals(">")) {
						if(ncpaid > credcan) {
							String response = objParallelC.getSegment();
							if(response.equals("R")) {
								persona.put("msgLN", objParallelC.getMessage());
	    				    	persona.put("idLN","Riesgo:"+irisk+", "+" Créditos pagados:"+ncpaid);
	    				    	persona.put("matrizReason","Matriz Índice de Riesgo");
							}
							return response;
						}
					}else if(validatorLogic.equals("=")) {
						if(ncpaid.equals(credcan)) {
							String response = objParallelC.getSegment();
							if(response.equals("R")) {
								persona.put("msgLN", objParallelC.getMessage());
	    				    	persona.put("idLN","Riesgo:"+irisk+", "+" Créditos pagados:"+ncpaid);
	    				    	persona.put("matrizReason","Matriz Índice de Riesgo");
							}
							return response;
						}
					}
				}
			}
		}
		
		// DEFAULT
		for (shppee_RiskIndex deafultObj : listObjRiskIndex) {
			if (deafultObj.isShppwsDefaultField()) {
				String response = deafultObj.getSegment();
				if (response.equals("R")) {
					persona.put("matrizReason", "Matriz Índice de Riesgo");
					persona.put("idLN", "Riesgo:"+irisk+", "+" Créditos pagados:"+ncpaid);
					persona.put("msgLN", deafultObj.getShppwsDefaultMessage() + "");
				}
				return response;
			}
		}
		if(persona.get("Mensaje_Operacional").equals("Error equifax")) {
			return getDefaultRiskIndiexEqError();
		}else {
		persona.put("msgLN", "NO EXISTEN DATOS EN LA MATRIZ DE INDICE DE RIESGO");
    	persona.put("idLN","Riesgo:"+irisk+", "+" Créditos pagados:"+ncpaid);
    	persona.put("matrizReason","Matriz Índice de Riesgo");
		return "R";
		}
	}
	
	
	  //
	 //Filtro Equifax
	//
	public String procesarApiEQ( BusinessPartner partner, shppws_config accesApi, String ID, Map<String, Object> persona, JSONObject jsonMonitor )throws JSONException {
			Double CV = 0.0; 
		    Double DJ = 0.0; 
		    Double CC = 0.0; 
		    Double inclusion= new Double(0);
			persona.put("CV", CV);
			persona.put("CC", CC);
			persona.put("DJ", DJ);
	        persona.put("EQ_segmentacion", "");
	        persona.put("EQ_score_inclusion", inclusion);
	        String responseEquifaxMessage="";
	        persona.put("Mensaje_Operacional", "");
	   //Se verifica existencia de equifax para el nuevo usuario	
				 persona.put("ApiResponse", "C");
				 try {
					OBCriteria<SweqxEquifax> queryequifax= OBDal.getInstance().createCriteria(SweqxEquifax.class);
					queryequifax.add(Restrictions.eq(SweqxEquifax.PROPERTY_BUSINESSPARTNER + ".id", partner.getId()));
					List<SweqxEquifax> equifaxs = queryequifax.list();
					int sizequifax=equifaxs.size();
					if(sizequifax > 0) {
						  Collections.sort(equifaxs, (e1, e2) -> e2.getCreationDate().compareTo(e1.getCreationDate()));
						  SweqxEquifax registroMasActual = equifaxs.get(0);
						  
						  LocalDate fechaActual = LocalDate.now();
						  String fechaCreacionString = registroMasActual.getCreationDate().toString();
						  DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
						  LocalDate fechaCreacion = LocalDate.parse(fechaCreacionString.substring(0, 10), formatter);
						  
						  int mesActual = fechaActual.getMonthValue();
						  int anioActual = fechaActual.getYear();
						  int mesCreacion = fechaCreacion.getMonthValue();
						  int anioCreacion = fechaCreacion.getYear();

						  
							  if (mesActual == mesCreacion && anioActual == anioCreacion) {  //VIGENCIA DE CONSULTA
								  EQoldSearch(registroMasActual, persona); //solo obtener CC, CV, DJ
							  } else { //Ya es vieja la consulta, se crea un nuevo Equifax
								String apiResponse = getApiResponse(accesApi, ID, 7, jsonMonitor);
								if(apiResponse.equals("Error equifax")) {
									persona.put("Exception_Institucion", "False");
									persona.put("Mensaje_Operacional", apiResponse);
									statusEquifax = false;
								}else {
									EQnewSearch(accesApi,apiResponse, partner, persona);
									persona.get("EQ_segmentacion");
									statusEquifax = true;
								}
								
							  }
					}else { //se crea un nuevo Euifax directamente, al saber que la lista esta vacia
						String apiResponse = getApiResponse(accesApi, ID, 7, jsonMonitor);
						if(apiResponse.equals("Error equifax")) {
							persona.put("Exception_Institucion", "False");
							persona.put("Mensaje_Operacional", apiResponse);
						}else {
							EQnewSearch(accesApi,apiResponse, partner, persona);
						}
					}
					
				 }catch(Exception e) {
				    	persona.put("msgLN", e.getMessage()+"");
				    	persona.put("idLN",ID);
				    	persona.put("matrizReason","Servicio Equifax");
				    	responseEquifaxMessage = e.getMessage()+"";
				    	responseEquifaxMessage = (responseEquifaxMessage == null || responseEquifaxMessage.equals("")) ? "No message" : responseEquifaxMessage;
				 }
				 return responseEquifaxMessage;
	}
	
	public void EQnewSearch(shppws_config accesApi, String apiResponse,BusinessPartner partner,  Map<String, Object> persona) throws JSONException {
		// Converts the JSON string to a JSONObject
		SweqxEquifax equifax = OBProvider.getInstance().get(SweqxEquifax.class);
		String equifaxValid = "Equifax";
		Boolean pass=false;
		String Segmento_Final="";
		  equifax.setClient(accesApi.getClient());
			equifax.setOrganization(accesApi.getOrganization());
			equifax.setActive(accesApi.isActive());
			equifax.setCreatedBy(accesApi.getCreatedBy());
	        equifax.setUpdatedBy(accesApi.getUpdatedBy());
	        
	        equifax.setBusinessPartner(partner);
	        equifax.setProfile(equifaxValid);
	        	Long auxEvaluation= new Long(0);
	        equifax.setEvaluation(auxEvaluation);
	        	String AmounttoFinance = (String)persona.get("Amount");
	        	BigDecimal auxfinancedValue= new BigDecimal(AmounttoFinance);
	        equifax.setFinancedValue(auxfinancedValue);
	        equifax.setProductType("Crédito");
	        equifax.setSegmentation("C");
	        equifax.setLinkJson(accesApi.getEQNamespace()+""+accesApi.getEQReadEndpoint());
	        equifax.setLinkXml(accesApi.getEQNamespace()+""+accesApi.getEQReadEndpoint());
	        
	        try {
				Gson gson = new GsonBuilder().setPrettyPrinting().create();
				String formattedJson = gson.toJson(gson.fromJson(apiResponse, Object.class));
				//if (formattedJson.length() > 4000) {formattedJson = formattedJson.substring(0, 4000);}
				equifax.setShppwsResultApi(formattedJson);
			    JsonObject jsonResponseEquifax = gson.fromJson(apiResponse, JsonObject.class);
			    JsonObject interconnect = jsonResponseEquifax.getAsJsonObject("interconnectResponse");
			    BigDecimal capacidadDePago = BigDecimal.ZERO;
			    BigDecimal gasto_financiero = BigDecimal.ZERO;
			    BigDecimal incomepredictor = BigDecimal.ZERO;
			    BigDecimal score_v4 = BigDecimal.ZERO;
			    if (interconnect.has("resultado")) {
		            JsonArray resultadoArray = interconnect.getAsJsonArray("resultado");

		            // Iterar sobre el array "RESULTADO"
		            for (JsonElement element : resultadoArray) {
		                JsonObject resultado = element.getAsJsonObject();
		                String variable = resultado.get("variable").getAsString().trim();
		                String resultadoValor = resultado.get("resultado").getAsString();

		                // Identificar las variables específicas y asignarlas
		                if (variable.equals("CAPACIDAD DE PAGO :")) {
		                    capacidadDePago = new BigDecimal(resultadoValor);
		                } else if (variable.equals("GASTO FINANCIERO :")) {
		                	gasto_financiero = new BigDecimal(resultadoValor);
		                } else if (variable.equals("INCOME PREDICTOR :")) {
		                	incomepredictor = new BigDecimal(resultadoValor);
		                } else if (variable.equals("SCORE V4 :")) {
		                	score_v4 = new BigDecimal(resultadoValor);
		                }
		            }
		        }

			    equifax.setShppetLoadcapacity(capacidadDePago);
			    equifax.setShppetFinancialExpense(gasto_financiero);
			    equifax.setShppetIncomePredictor(incomepredictor);
			    equifax.setShppetScoreV4(score_v4);
			} catch (Exception e) {}
	        
	    OBDal.getInstance().save(equifax);
		OBDal.getInstance().flush();
		
		
		//MAIN Equifax
		Double CV = new Double (0); // Total Vencido
	    Double DJ = new Double (0); // Dem. Jud.
	    Double CC = new Double (0); // Cart. Cast.
	    Double TV= new Double(0); // Total Vencer
	    Double NDI= new Double(0); //NDI
	    Double SD= new Double(0); //Saldo Deuda
		
		///DetalleDeudaActualReportadaSICOM360///
		try {
	    JSONObject jsonResponse = new JSONObject(apiResponse);
	    JSONArray detalleDeudaArray = null;
	    if(jsonResponse.has("reporteCrediticio")) {
	    	JSONObject reporteCrediticio = jsonResponse.getJSONObject("reporteCrediticio");

		    	if(reporteCrediticio.has("detalle_deuda_actual_sicom")) {
		    		detalleDeudaArray = reporteCrediticio.getJSONArray("detalle_deuda_actual_sicom");
			    	    if (detalleDeudaArray != null && detalleDeudaArray.length() > 0) {
				    	if(detalleDeudaArray.getJSONObject(0).has("institucion")) {
						 for (int i = 0; i < detalleDeudaArray.length(); i++) {
						     JSONObject detalleDeuda = detalleDeudaArray.getJSONObject(i);
						     CV += detalleDeuda.getDouble("total_vencido");
						     DJ += detalleDeuda.getDouble("dem_jud");
						     CC += detalleDeuda.getDouble("cart_cast");
						     TV += detalleDeuda.getDouble("total_vencer");
							 NDI += detalleDeuda.getDouble("ndi");
							 SD += detalleDeuda.getDouble("saldo_deuda");
						     
						     try {//lines detail
						    	 OBCriteria<InstexceptionEqfx> queryApi_Instexception_eqfx = OBDal.getInstance().createCriteria(InstexceptionEqfx.class);
								 queryApi_Instexception_eqfx.add(Restrictions.eq(InstexceptionEqfx.PROPERTY_CREDITOR,(String)detalleDeuda.get("institucion")));
								 queryApi_Instexception_eqfx.add(Restrictions.eq(InstexceptionEqfx.PROPERTY_ACTIVE,true));
								 queryApi_Instexception_eqfx.setMaxResults(1);
								 List<InstexceptionEqfx> listOp =  queryApi_Instexception_eqfx.list();
						    	 if(listOp.size()>0) {
						    		 //valores de ventana
						    		 InstexceptionEqfx Instexception_eqfx = queryApi_Instexception_eqfx.list().get(0);
						    		 BigDecimal Rango_Inf_Valor_Vencido=Instexception_eqfx.getLowerRankExpdvalue();
						    		 BigDecimal Rango_Sup_Valor_Vencido=Instexception_eqfx.getUpperRankExpdvalue(); 
						    		 BigDecimal Rango_Inf_Cartera_Castigada=Instexception_eqfx.getLowerRankPenportfolio(); 
						    		 BigDecimal Rango_Sup_Cartera_Castigada=Instexception_eqfx.getUpperRankPenportfolio(); 
						    		 BigDecimal Rango_Inf_Demanda_Judicial=Instexception_eqfx.getLowerRankJudClaim(); 
						    		 BigDecimal Rango_Sup_Demanda_Judicial=Instexception_eqfx.getUpperRankJudClaim(); 
						    		 Segmento_Final=Instexception_eqfx.getFinalSegment(); 
						    		 //valores de api
						    		 Double total_vencido=detalleDeuda.getDouble("total_vencido");
						    		 Double cartera_castigada=detalleDeuda.getDouble("cart_cast");
						    		 Double demanda_judicial=detalleDeuda.getDouble("dem_jud");
						    		 if (new BigDecimal(total_vencido).compareTo(Rango_Inf_Valor_Vencido) >= 0 && 
						    				 new BigDecimal(total_vencido).compareTo(Rango_Sup_Valor_Vencido) <= 0 &&
							    			 new BigDecimal(cartera_castigada).compareTo(Rango_Inf_Cartera_Castigada) >= 0 && 
								    		 new BigDecimal(cartera_castigada).compareTo(Rango_Sup_Cartera_Castigada) <= 0 &&
								    		 new BigDecimal(demanda_judicial).compareTo(Rango_Inf_Demanda_Judicial) >= 0 && 
								    		 new BigDecimal(demanda_judicial).compareTo(Rango_Sup_Demanda_Judicial) <= 0) {
		        							 persona.put("Segment",Segmento_Final);
		        							 persona.put("Exception_Institucion","true");
		        							 pass=true;
						    		 }else {
								   		    	persona.put("Exception_Institucion","false");
								   		     }
									   		 if(pass=true) {
									   			 shppws_detailDue detailDue = OBProvider.getInstance().get(shppws_detailDue.class);
										    	 detailDue.setClient(accesApi.getClient());
										    	 detailDue.setOrganization(accesApi.getOrganization());
										         detailDue.setActive(accesApi.isActive());
										         detailDue.setCreatedBy(accesApi.getCreatedBy());
										         detailDue.setUpdatedBy(accesApi.getUpdatedBy());	
										         detailDue.setSweqxEquifax(equifax);
													
										         try{detailDue.setInstitution((String)detalleDeuda.get("institucion"));}catch(Exception e) {}
										         try{detailDue.setCutoffDate((String)detalleDeuda.get("fecha_corte"));}catch(Exception e) {}
										         try{detailDue.setRiskType((String)detalleDeuda.get("tipo_riesgo"));}catch(Exception e) {}
										         try{detailDue.setCreditType((String)detalleDeuda.get("tipo_credito"));}catch(Exception e) {}
										         try{Double cupoMonto = detalleDeuda.getDouble("cupo_monto_original");
										         detailDue.setQuotaAmount(String.valueOf(cupoMonto));}catch(Exception e) {}
										         try{detailDue.setOpeningDate((String)detalleDeuda.get("fecha_apertura"));}catch(Exception e) {}
										         try{detailDue.setDueDate((String)detalleDeuda.get("fecha_vencimiento"));}catch(Exception e) {}
										         try{detailDue.setOwnrating((String)detalleDeuda.get("Calif. Propia"));}catch(Exception e) {} // No se usa en la nueva estructura de equifax, no afecta funcionalidad
										         try{Double totalVencer = detalleDeuda.getDouble("total_vencer");
										         detailDue.setTotalBeat(String.valueOf(totalVencer));}catch(Exception e) {}
										         try{Double NDI2 = detalleDeuda.getDouble("ndi");
										         detailDue.setNDI(String.valueOf(NDI2));}catch(Exception e) {}
										         try{Double totalVencido = detalleDeuda.getDouble("total_vencido");
										         detailDue.setTotalOverdue(String.valueOf(totalVencido));}catch(Exception e) {}
										         try{Double demJud = detalleDeuda.getDouble("dem_jud");
										         detailDue.setDemJud(String.valueOf(demJud));}catch(Exception e) {}
										         try{Double cartCast = detalleDeuda.getDouble("cart_cast");
										         detailDue.setCartCast(String.valueOf(cartCast));}catch(Exception e) {}
										         try{Double saldoDeuda = detalleDeuda.getDouble("saldo_deuda");
										         detailDue.setDebtBalance(String.valueOf(saldoDeuda));}catch(Exception e) {}
										         try{Double cuotaMensual = detalleDeuda.getDouble("cuota_mensual");
										         detailDue.setMonthlyFee(String.valueOf(cuotaMensual));}catch(Exception e) {}
										         detailDue.setMaxDaysDue("maxDays_due");
													
										         OBDal.getInstance().save(detailDue);
										         OBDal.getInstance().flush();
										    	 }else {
											    		persona.put("Exception_Institucion","false");
										    	 }
						    	 }else {
						    		 shppws_detailDue detailDue = OBProvider.getInstance().get(shppws_detailDue.class);
							    	 detailDue.setClient(accesApi.getClient());
							    	 detailDue.setOrganization(accesApi.getOrganization());
							         detailDue.setActive(accesApi.isActive());
							         detailDue.setCreatedBy(accesApi.getCreatedBy());
							         detailDue.setUpdatedBy(accesApi.getUpdatedBy());	
							         detailDue.setSweqxEquifax(equifax);
										
							         try{detailDue.setInstitution((String)detalleDeuda.get("institucion"));}catch(Exception e) {}
							         try{detailDue.setCutoffDate((String)detalleDeuda.get("fecha_corte"));}catch(Exception e) {}
							         try{detailDue.setRiskType((String)detalleDeuda.get("tipo_riesgo"));}catch(Exception e) {}
							         try{detailDue.setCreditType((String)detalleDeuda.get("tipo_credito"));}catch(Exception e) {}
							         try{Double cupoMonto = detalleDeuda.getDouble("cupo_monto_original");
							         detailDue.setQuotaAmount(String.valueOf(cupoMonto));}catch(Exception e) {}
							         try{detailDue.setOpeningDate((String)detalleDeuda.get("fecha_apertura"));}catch(Exception e) {}
							         try{detailDue.setDueDate((String)detalleDeuda.get("fecha_vencimiento"));}catch(Exception e) {}
							         try{detailDue.setOwnrating((String)detalleDeuda.get("Calif. Propia"));}catch(Exception e) {} // No se usa en la nueva estructura de equifax, no afecta funcionalidad
							         try{Double totalVencer = detalleDeuda.getDouble("total_vencer");
							         detailDue.setTotalBeat(String.valueOf(totalVencer));}catch(Exception e) {}
							         try{Double NDI2 = detalleDeuda.getDouble("ndi");
							         detailDue.setNDI(String.valueOf(NDI2));}catch(Exception e) {}
							         try{Double totalVencido = detalleDeuda.getDouble("total_vencido");
							         detailDue.setTotalOverdue(String.valueOf(totalVencido));}catch(Exception e) {}
							         try{Double demJud = detalleDeuda.getDouble("dem_jud");
							         detailDue.setDemJud(String.valueOf(demJud));}catch(Exception e) {}
							         try{Double cartCast = detalleDeuda.getDouble("cart_cast");
							         detailDue.setCartCast(String.valueOf(cartCast));}catch(Exception e) {}
							         try{Double saldoDeuda = detalleDeuda.getDouble("saldo_deuda");
							         detailDue.setDebtBalance(String.valueOf(saldoDeuda));}catch(Exception e) {}
							         try{Double cuotaMensual = detalleDeuda.getDouble("cuota_mensual");
							         detailDue.setMonthlyFee(String.valueOf(cuotaMensual));}catch(Exception e) {}
							         detailDue.setMaxDaysDue("maxDays_due");
										
							         OBDal.getInstance().save(detailDue);
							         OBDal.getInstance().flush();
							    		persona.put("Exception_Institucion","false");
						    	 }
						     
						     }catch(Exception e) {
						    	 e.getMessage();
						     }
						 }
						    }else {
					    		persona.put("Exception_Institucion","false");
						    }
					    }else {
				    		persona.put("Exception_Institucion","false");
					    }
			    	    	
			    }else {
			    	jsonResponse = new JSONObject(apiResponse);
		    		persona.put("Exception_Institucion","false");
		    		detalleDeudaArray = reporteCrediticio.getJSONArray("detalle_deuda_actual_sicom");
				for (int i = 0; i < detalleDeudaArray.length(); i++) {
					     JSONObject detalleDeuda = detalleDeudaArray.getJSONObject(i);
					     CV += detalleDeuda.getDouble("total_vencido");
					     DJ += detalleDeuda.getDouble("dem_jud");
					     CC += detalleDeuda.getDouble("cart_cast");
					      TV += detalleDeuda.getDouble("total_vencer");
						  NDI += detalleDeuda.getDouble("ndi");
						  SD += detalleDeuda.getDouble("saldo_deuda");
					     
					     try {//lines detail
					     shppws_detailDue detailDue = OBProvider.getInstance().get(shppws_detailDue.class);
					        detailDue.setClient(accesApi.getClient());
							detailDue.setOrganization(accesApi.getOrganization());
							detailDue.setActive(accesApi.isActive());
							detailDue.setCreatedBy(accesApi.getCreatedBy());
							detailDue.setUpdatedBy(accesApi.getUpdatedBy());	
							detailDue.setSweqxEquifax(equifax);
							
							try{detailDue.setInstitution((String)detalleDeuda.get("institucion"));}catch(Exception e) {}
							try{detailDue.setCutoffDate((String)detalleDeuda.get("fecha_corte"));}catch(Exception e) {}
							try{detailDue.setRiskType((String)detalleDeuda.get("tipo_riesgo"));}catch(Exception e) {}
							try{detailDue.setCreditType((String)detalleDeuda.get("tipo_credito"));}catch(Exception e) {}
								try{Double cupoMonto = detalleDeuda.getDouble("cupo_monto_original");
							    detailDue.setQuotaAmount(String.valueOf(cupoMonto));}catch(Exception e) {}
							try{detailDue.setOpeningDate((String)detalleDeuda.get("fecha_apertura"));}catch(Exception e) {}
							try{detailDue.setDueDate((String)detalleDeuda.get("fecha_vencimiento"));}catch(Exception e) {}
							try{detailDue.setOwnrating((String)detalleDeuda.get("Calif. Propia"));}catch(Exception e) {}  // No se usa en la nueva estructura de equifax, no afecta funcionalidad
								try{Double totalVencer = detalleDeuda.getDouble("total_vencer");
							detailDue.setTotalBeat(String.valueOf(totalVencer));}catch(Exception e) {}
								try{Double NDI2 = detalleDeuda.getDouble("ndi");
							detailDue.setNDI(String.valueOf(NDI2));}catch(Exception e) {}
								try{Double totalVencido = detalleDeuda.getDouble("total_vencido");
							detailDue.setTotalOverdue(String.valueOf(totalVencido));}catch(Exception e) {}
								try{Double demJud = detalleDeuda.getDouble("dem_jud");
							detailDue.setDemJud(String.valueOf(demJud));}catch(Exception e) {}
								try{Double cartCast = detalleDeuda.getDouble("cart_cast");
							detailDue.setCartCast(String.valueOf(cartCast));}catch(Exception e) {}
								try{Double saldoDeuda = detalleDeuda.getDouble("saldo_deuda");
							detailDue.setDebtBalance(String.valueOf(saldoDeuda));}catch(Exception e) {}
								try{Double cuotaMensual = detalleDeuda.getDouble("cuota_mensual");
							detailDue.setMonthlyFee(String.valueOf(cuotaMensual));}catch(Exception e) {}
							detailDue.setMaxDaysDue("maxDays_due");
							
						OBDal.getInstance().save(detailDue);
						OBDal.getInstance().flush();
					     }catch(Exception e) {
					    	 e.getMessage();
					     }
					 }
			    }
		    }else {
		    	persona.put("Exception_Institucion","false");
		    }
		}catch(Exception e) {
		}
		
		///DetalleDeudaActualReportadaSBS360///
		try {
	    JSONObject jsonResponse = new JSONObject(apiResponse);
	    if(jsonResponse.has("reporteCrediticio")) {
		    JSONObject reporteCrediticio = jsonResponse.getJSONObject("reporteCrediticio");
		    JSONArray detalleDeudaArray = reporteCrediticio.getJSONArray("detalle_deuda_actual_sb");
					 for (int i = 0; i < detalleDeudaArray.length(); i++) {
					     JSONObject detalleDeuda = detalleDeudaArray.getJSONObject(i);
					     CV += detalleDeuda.getDouble("total_vencido");
					     DJ += detalleDeuda.getDouble("dem_jud");
					     CC += detalleDeuda.getDouble("cart_cast");
					     TV += detalleDeuda.getDouble("total_vencer");
						 NDI += detalleDeuda.getDouble("ndi");
						 SD += detalleDeuda.getDouble("saldo_deuda");
					     
					     try {//lines detail
					     shppws_detailDue detailDue = OBProvider.getInstance().get(shppws_detailDue.class);
					        detailDue.setClient(accesApi.getClient());
							detailDue.setOrganization(accesApi.getOrganization());
							detailDue.setActive(accesApi.isActive());
							detailDue.setCreatedBy(accesApi.getCreatedBy());
							detailDue.setUpdatedBy(accesApi.getUpdatedBy());	
							detailDue.setSweqxEquifax(equifax);
							
							try{detailDue.setInstitution((String)detalleDeuda.get("institucion"));}catch(Exception e) {}
							try{detailDue.setCutoffDate((String)detalleDeuda.get("fecha_corte"));}catch(Exception e) {}
							try{detailDue.setRiskType((String)detalleDeuda.get("tipo_riesgo"));}catch(Exception e) {}
							try{detailDue.setCreditType((String)detalleDeuda.get("tipo_credito"));}catch(Exception e) {}
								try{Double cupoMonto = detalleDeuda.getDouble("cupo_monto_original");
							    detailDue.setQuotaAmount(String.valueOf(cupoMonto));}catch(Exception e) {}
							try{detailDue.setOpeningDate((String)detalleDeuda.get("fecha_apertura"));}catch(Exception e) {}
							try{detailDue.setDueDate((String)detalleDeuda.get("fecha_vencimiento"));}catch(Exception e) {}
							try{detailDue.setOwnrating((String)detalleDeuda.get("Calif. Propia"));}catch(Exception e) {}
								try{Double totalVencer = detalleDeuda.getDouble("total_vencer");
							detailDue.setTotalBeat(String.valueOf(totalVencer));}catch(Exception e) {}
								try{Double NDI2 = detalleDeuda.getDouble("ndi");
							detailDue.setNDI(String.valueOf(NDI2));}catch(Exception e) {}
								try{Double totalVencido = detalleDeuda.getDouble("total_vencido");
							detailDue.setTotalOverdue(String.valueOf(totalVencido));}catch(Exception e) {}
								try{Double demJud = detalleDeuda.getDouble("dem_jud");
							detailDue.setDemJud(String.valueOf(demJud));}catch(Exception e) {}
								try{Double cartCast = detalleDeuda.getDouble("cart_cast");
							detailDue.setCartCast(String.valueOf(cartCast));}catch(Exception e) {}
								try{Double saldoDeuda = detalleDeuda.getDouble("saldo_deuda");
							detailDue.setDebtBalance(String.valueOf(saldoDeuda));}catch(Exception e) {}
								try{Double cuotaMensual = detalleDeuda.getDouble("cuota_mensual");
							detailDue.setMonthlyFee(String.valueOf(cuotaMensual));}catch(Exception e) {}
							detailDue.setMaxDaysDue("maxDays_due");
							
						OBDal.getInstance().save(detailDue);
						OBDal.getInstance().flush();
					     }catch(Exception e) {
					    	 e.getMessage();
					     }
					 }
			}else {
	        	persona.put("Exception_Institucion","false");
	        }
		}catch(Exception e) {
		}
		
		///DetalleDeudaActualReportadaRFR360///
		try {
	    JSONObject jsonResponse = new JSONObject(apiResponse);

	    if(jsonResponse.has("reporteCrediticio")) {
		    JSONObject reporteCrediticio = jsonResponse.getJSONObject("reporteCrediticio");

		    JSONArray detalleDeudaArray = reporteCrediticio.getJSONArray("detalle_deuda_actual_seps");
					 for (int i = 0; i < detalleDeudaArray.length(); i++) {
					     JSONObject detalleDeuda = detalleDeudaArray.getJSONObject(i);
					     CV += detalleDeuda.getDouble("total_vencido");
					     DJ += detalleDeuda.getDouble("dem_jud");
					     CC += detalleDeuda.getDouble("cart_cast");
					     TV += detalleDeuda.getDouble("total_vencer");
						 NDI += detalleDeuda.getDouble("ndi");
						 SD += detalleDeuda.getDouble("saldo_deuda");
					     
					     try {//lines detail
					     shppws_detailDue detailDue = OBProvider.getInstance().get(shppws_detailDue.class);
					        detailDue.setClient(accesApi.getClient());
							detailDue.setOrganization(accesApi.getOrganization());
							detailDue.setActive(accesApi.isActive());
							detailDue.setCreatedBy(accesApi.getCreatedBy());
							detailDue.setUpdatedBy(accesApi.getUpdatedBy());	
							detailDue.setSweqxEquifax(equifax);
							
							try{detailDue.setInstitution((String)detalleDeuda.get("institucion"));}catch(Exception e) {}
							try{detailDue.setCutoffDate((String)detalleDeuda.get("fecha_corte"));}catch(Exception e) {}
							try{detailDue.setRiskType((String)detalleDeuda.get("tipo_riesgo"));}catch(Exception e) {}
							try{detailDue.setCreditType((String)detalleDeuda.get("tipo_credito"));}catch(Exception e) {}
								try{Double cupoMonto = detalleDeuda.getDouble("cupo_monto_original");
							    detailDue.setQuotaAmount(String.valueOf(cupoMonto));}catch(Exception e) {}
							try{detailDue.setOpeningDate((String)detalleDeuda.get("fecha_apertura"));}catch(Exception e) {}
							try{detailDue.setDueDate((String)detalleDeuda.get("fecha_vencimiento"));}catch(Exception e) {}
							try{detailDue.setOwnrating((String)detalleDeuda.get("Calif. Propia"));}catch(Exception e) {}
								try{Double totalVencer = detalleDeuda.getDouble("total_vencer");
							detailDue.setTotalBeat(String.valueOf(totalVencer));}catch(Exception e) {}
								try{Double NDI2 = detalleDeuda.getDouble("ndi");
							detailDue.setNDI(String.valueOf(NDI2));}catch(Exception e) {}
								try{Double totalVencido = detalleDeuda.getDouble("total_vencido");
							detailDue.setTotalOverdue(String.valueOf(totalVencido));}catch(Exception e) {}
								try{Double demJud = detalleDeuda.getDouble("dem_jud");
							detailDue.setDemJud(String.valueOf(demJud));}catch(Exception e) {}
								try{Double cartCast = detalleDeuda.getDouble("cart_cast");
							detailDue.setCartCast(String.valueOf(cartCast));}catch(Exception e) {}
								try{Double saldoDeuda = detalleDeuda.getDouble("saldo_deuda");
							detailDue.setDebtBalance(String.valueOf(saldoDeuda));}catch(Exception e) {}
								try{Double cuotaMensual = detalleDeuda.getDouble("cuota_mensual");
							detailDue.setMonthlyFee(String.valueOf(cuotaMensual));}catch(Exception e) {}
							detailDue.setMaxDaysDue("maxDays_due");
							
						OBDal.getInstance().save(detailDue);
						OBDal.getInstance().flush();
					     }catch(Exception e) {
					    	 e.getMessage();
					     }
					 }
			}else {
	        	persona.put("Exception_Institucion","false");
			}
		}catch(Exception e) {
		}
		
		
		
		//Save results
		try {
			 equifax.setShppetTotalBeat(new BigDecimal(TV));//total_vencer
			 equifax.setShppetNdi(new BigDecimal(NDI));//NDI
			 equifax.setShppetTotalOverdue(new BigDecimal(CV));//total_vencido
			 equifax.setShppetDemJud(new BigDecimal(DJ));//dem_jud
			 equifax.setShppetCartCast(new BigDecimal(CC));//cart_cast
			 equifax.setShppetDebtBalance(new BigDecimal(SD));//saldo_deuda
			 OBDal.getInstance().save(equifax);
			 OBDal.getInstance().flush();
			 
		   persona.put("CV", CV);
		   persona.put("CC", CC);
		   persona.put("DJ", DJ);
		}catch(Exception e) {
			
		}
	    
		
		//GET SEGMENT and SAVE 
		try {
			JSONObject jsonResponse = new JSONObject(apiResponse);
	    JSONObject interconnect = jsonResponse.getJSONObject("interconnectResponse");
	        JSONArray resultadoArray = interconnect.getJSONArray("resultado");
		    String segmentacion = null;
			     for (int i = 0; i < resultadoArray.length(); i++) {
			         JSONObject resultado = resultadoArray.getJSONObject(i);
			         String variable = resultado.getString("variable");
			         String resultadoValor = resultado.getString("resultado");
			         if(pass) {
			        	 //resultadoValor=Segmento_Final;
			        	 	if (variable.equals("SEGMENTACION BANCARIZADO:")) {
					        	 segmentacion = (resultadoValor != null && !(resultadoValor.equals(""))) ? resultadoValor : "OTRO"; //when Resultado is empty
					        	 break;
					         }
						}if(!pass) {
					         if (variable.equals("SEGMENTACION BANCARIZADO:")) {
					        	 segmentacion = (resultadoValor != null && !(resultadoValor.equals(""))) ? resultadoValor : "OTRO"; //when Resultado is empty
					        	 break;
					         }

						}
			     }
			     
			     segmentacion = (segmentacion == null || segmentacion.equals("")) ? "OTRO":segmentacion; //when not find SEGMENTACION :
			     
			     equifax.setSegmentation(segmentacion);
	             OBDal.getInstance().save(equifax);
				 OBDal.getInstance().flush();
			     
	        persona.put("EQ_segmentacion", segmentacion);
		}catch(Exception e) {
			equifax.setSegmentation("OTRO");
			OBDal.getInstance().save(equifax);
			OBDal.getInstance().flush();
	        persona.put("EQ_segmentacion", "OTRO");	
		}   
		
		
		
		
		//GET SCORE_INCLUSION and SAVE 
		try {
			JSONObject jsonResponse = new JSONObject(apiResponse);
			JSONObject interconnect = jsonResponse.getJSONObject("interconnectResponse");
	        JSONArray resultadoArray = interconnect.getJSONArray("resultado");
			Double inclusion= new Double(-999999);
			     for (int i = 0; i < resultadoArray.length(); i++) {
			         JSONObject resultado = resultadoArray.getJSONObject(i);
			         String variable = resultado.getString("variable");
			         String resultadoValor = resultado.getString("resultado");
		
			         if (variable.equals("SCORE INCLUSION :")) {
			             if(resultadoValor.equals("SIN SCORE")) {
			            	 inclusion = new Double(0);
							 break;
			             } else {
			            	 inclusion = Double.parseDouble(resultadoValor);
							 break;
			             }
			         }
			     }
			     
			     inclusion = (inclusion >= 0) ? inclusion : new Double(999999);
			     
			     Long auxEvaluation1 = inclusion.longValue();
			     equifax.setEvaluation(auxEvaluation1);
            	 OBDal.getInstance().save(equifax);
				 OBDal.getInstance().flush();
			        	
	        
	        persona.put("EQ_score_inclusion", inclusion);
		}catch(Exception e) {
			Double inclusion= new Double(999999);
			Long auxEvaluation1 = inclusion.longValue();
		    equifax.setEvaluation(auxEvaluation1);
		    OBDal.getInstance().save(equifax);
			OBDal.getInstance().flush();
	        persona.put("EQ_score_inclusion", inclusion);
		}
		
		// Guardar informacion demografica
		try {
			JSONObject rootObj = new JSONObject(apiResponse);

		    if(rootObj.has("reporteCrediticio")) {
			JSONObject reporteCrediticio = rootObj.optJSONObject("reporteCrediticio");
			if (reporteCrediticio != null) {
				JSONArray infoDemo = reporteCrediticio.optJSONArray("informacion_demografica");
				if (infoDemo != null && infoDemo.length() > 0) {
					JSONObject d = infoDemo.optJSONObject(0);
					if (d != null) {
						try { equifax.setShppwsEducationLevel(d.getString("educacion")); } catch (Exception ignore) {}
						try { equifax.setShppwsProvince(d.getString("provincia")); } catch (Exception ignore) {}
						try { equifax.setShppwsCanton(d.getString("canton")); } catch (Exception ignore) {}
						try { equifax.setShppwsAddresses(d.getString("direcciones")); } catch (Exception ignore) {}
						try { equifax.setShppwsCoordinateX(d.getString("coordenada_x")); } catch (Exception ignore) {}
						try { equifax.setShppwsCoordinateY(d.getString("coordenada_y")); } catch (Exception ignore) {}
						try { equifax.setShppwsConventionalPhone(d.getString("numero_telefonico_convencional")); } catch (Exception ignore) {}
						try { equifax.setShppwsCellPhone(d.getString("numero_telefonico_celular")); } catch (Exception ignore) {}
						try {
							String fn = d.getString("fecha_nacimiento");
							if (fn != null && !fn.isEmpty()) {
								Date parsed = null;
								try {
									SimpleDateFormat sdfEs = new SimpleDateFormat("d 'de' MMMM 'de' yyyy", new Locale("es", "ES"));
									parsed = sdfEs.parse(fn);
								} catch (Exception e1) {
									try {
										SimpleDateFormat sdfAlt = new SimpleDateFormat("dd/MM/yyyy");
										parsed = sdfAlt.parse(fn);
									} catch (Exception e2) {}
								}
								if (parsed != null) { equifax.setShppwsBirthDate(parsed); }
							}
						} catch (Exception ignore) {}
						// Persistir informacion demográfica
						try { OBDal.getInstance().save(equifax); OBDal.getInstance().flush(); } catch (Exception ignore) {}
					}
				}
			}
		}else {
			persona.put("Exception_Institucion","false");
		}
			} catch (Exception ignore) {}
			
			
		// Guardar resultado inclusion
		try {
		    JSONObject rootObj = new JSONObject(apiResponse);
		    JSONObject interconnect = rootObj.getJSONObject("interconnectResponse");
		    if (interconnect != null) {
		        JSONArray resultadoInclusion = interconnect.optJSONArray("resultado_inclusion");
		        if (resultadoInclusion != null && resultadoInclusion.length() > 0) {
		            for (int i = 0; i < resultadoInclusion.length(); i++) {
		                JSONObject rsi = resultadoInclusion.optJSONObject(i);
		                if (rsi != null && "SCORE INCLUSION".equalsIgnoreCase(rsi.optString("politica"))) {
		                    // Solo si la política es SCORE INCLUSION
		                    try { equifax.setShppwsInclusionValue(new BigDecimal(rsi.optString("valor", "0"))); } catch (Exception ignore) {}
		                    try { equifax.setShppwsInclusionDecision(rsi.optString("decision", null)); } catch (Exception ignore) {}
		                    
		                    // Persistir información score
		                    try { 
		                        OBDal.getInstance().save(equifax); 
		                        OBDal.getInstance().flush(); 
		                    } catch (Exception ignore) {}
		                    
		                    break;
		                }
		            }
		        }
		    }
		} catch (Exception ignore) {}

	}
	
	
	public void EQoldSearch(SweqxEquifax registroMasActual, Map<String, Object> persona) throws JSONException {
		try {
		String segmentation=registroMasActual.getSegmentation();
		Long inclusion=registroMasActual.getEvaluation();
		Double inclusionDouble = inclusion.doubleValue();
		persona.put("EQ_segmentacion", segmentation);	
        persona.put("EQ_score_inclusion", inclusionDouble);
		
		
		OBCriteria<shppws_detailDue> querydetail= OBDal.getInstance().createCriteria(shppws_detailDue.class);
		querydetail.add(Restrictions.eq(shppws_detailDue.PROPERTY_SWEQXEQUIFAX, registroMasActual));
		List<shppws_detailDue> duedetails = querydetail.list();
		Double CC = new Double (0); 
		Double CV = new Double (0);
	    Double DJ = new Double (0); 
	    
	    for (shppws_detailDue duedetail : duedetails) {
	        String StringCC = duedetail.getCartCast();
	        String StringCV = duedetail.getTotalOverdue();
	        String StringDJ = duedetail.getDemJud();

	        CC += Double.parseDouble(StringCC);
	        CV += Double.parseDouble(StringCV);
	        DJ += Double.parseDouble(StringDJ);
	    }
	    persona.put("CV", CV);
        persona.put("CC", CC);
        persona.put("DJ", DJ);
        
        try {
        	JSONObject jsonResponse = new JSONObject(registroMasActual.getShppwsResultApi());
        	JSONArray detalleDeudaArray = null;
    		String Segmento_Final="";
			Boolean pass=false;
			if(jsonResponse.has("reporteCrediticio")) {
			    JSONObject reporteCrediticio = jsonResponse.getJSONObject("reporteCrediticio");
	        		if(reporteCrediticio.has("detalle_deuda_actual_sicom")) {
	        			detalleDeudaArray = reporteCrediticio.getJSONArray("detalle_deuda_actual_sicom");
			    	    if (detalleDeudaArray != null && detalleDeudaArray.length() > 0) {
	        			if(detalleDeudaArray.getJSONObject(0).has("institucion")) {
	        				for (int i = 0; i < detalleDeudaArray.length(); i++) {
	   					     JSONObject detalleDeuda = detalleDeudaArray.getJSONObject(i);
	        				try {//lines detail
	        				OBCriteria<InstexceptionEqfx> queryApi_Instexception_eqfx = OBDal.getInstance().createCriteria(InstexceptionEqfx.class);
	        				queryApi_Instexception_eqfx.add(Restrictions.eq(InstexceptionEqfx.PROPERTY_CREDITOR,(String)detalleDeuda.get("institucion")));
	        				queryApi_Instexception_eqfx.add(Restrictions.eq(InstexceptionEqfx.PROPERTY_ACTIVE,true));
	        				queryApi_Instexception_eqfx.setMaxResults(1);
	        				List<InstexceptionEqfx> listOp =  queryApi_Instexception_eqfx.list();
	        					if(listOp.size()>0) {
	        						//valores de ventana
	        						InstexceptionEqfx Instexception_eqfx = queryApi_Instexception_eqfx.list().get(0);
	        						BigDecimal Rango_Inf_Valor_Vencido=Instexception_eqfx.getLowerRankExpdvalue();
	        						BigDecimal Rango_Sup_Valor_Vencido=Instexception_eqfx.getUpperRankExpdvalue(); 
	        						BigDecimal Rango_Inf_Cartera_Castigada=Instexception_eqfx.getLowerRankPenportfolio(); 
	        						BigDecimal Rango_Sup_Cartera_Castigada=Instexception_eqfx.getUpperRankPenportfolio(); 
	        						BigDecimal Rango_Inf_Demanda_Judicial=Instexception_eqfx.getLowerRankJudClaim(); 
	        						BigDecimal Rango_Sup_Demanda_Judicial=Instexception_eqfx.getUpperRankJudClaim(); 
	        						Segmento_Final=Instexception_eqfx.getFinalSegment(); 
	        						//valores de api
	        						Double total_vencido=detalleDeuda.getDouble("total_vencido");
	        						Double cartera_castigada=detalleDeuda.getDouble("cart_cast");
	        						Double demanda_judicial=detalleDeuda.getDouble("dem_jud");
	        						if (new BigDecimal(total_vencido).compareTo(Rango_Inf_Valor_Vencido) >= 0 && 
	        						new BigDecimal(total_vencido).compareTo(Rango_Sup_Valor_Vencido) <= 0 &&
	        						new BigDecimal(cartera_castigada).compareTo(Rango_Inf_Cartera_Castigada) >= 0 && 
	        						new BigDecimal(cartera_castigada).compareTo(Rango_Sup_Cartera_Castigada) <= 0 &&
	        						new BigDecimal(demanda_judicial).compareTo(Rango_Inf_Demanda_Judicial) >= 0 && 
	        						new BigDecimal(demanda_judicial).compareTo(Rango_Sup_Demanda_Judicial) <= 0) {
	        							persona.put("Segment",Segmento_Final);
		       							 persona.put("Exception_Institucion","true");
		       							 pass=true;
		        						}else {
		        							persona.put("Exception_Institucion","false");	
		        						}
		        					}else {
										persona.put("Exception_Institucion","false");
		        					}
	        				}catch(Exception e) {
	        					e.getMessage();
	        				}
	        			}
		        		}
		        		}else {
							persona.put("Exception_Institucion","false");
		        		}
	         		}else {
						persona.put("Exception_Institucion","false");
	        		}
				}else {
					persona.put("Exception_Institucion","false");
				}
        	}catch(Exception e) {
        	}
        
		}catch(Exception e) {}
	}
	

	public void validateEquifaxArrays( String ID, String Store_group, Map<String, Object> persona )throws JSONException {
		Double CV =(Double) persona.get("CV");
		Double CC =(Double) persona.get("CC");
		Double DJ =(Double) persona.get("DJ");
		//Double auxValuesSummary= CV+CC+DJ;
		
		//////VALORES VENCIDOS	     
		Boolean validateDefaultCV=true;
		OBCriteria<Shppec_ExpActual> matriz1= OBDal.getInstance().createCriteria(Shppec_ExpActual.class); 
	    List<Shppec_ExpActual> expiredValues = matriz1.list();
	    String segment1ExpiredValues="-";
        for (Shppec_ExpActual expiredValue : expiredValues) {
            if (Store_group.equals(expiredValue.getAgenCode())) {
            	
            	BigDecimal startValue = expiredValue.getValueFrom();
            	BigDecimal endValue = expiredValue.getValueUntil();
            	Double valueInitial = startValue.doubleValue();
            	Double valueFinal = endValue.doubleValue();
            	
                String answer = expiredValue.getSegment();
                if (CV >= valueInitial && CV <= valueFinal) {
                	validateDefaultCV = false;
                	segment1ExpiredValues=answer;
                	if(answer.equals("R")) {
                	 persona.put("matriz","Valor Vencido Actual EQFX");
   			    	 persona.put("matrizReason","Valor Vencido Actual EQFX");
   			    	 persona.put("msgLN", expiredValue.getMessage()+"");
   			    	 persona.put("idLN",CV+"");
   			    	 break; 
                	}
                	 
                }
                //break; 
            }
        }
        
        // DEFAULT
        if(validateDefaultCV) {
        	for (Shppec_ExpActual defaultObj : expiredValues) {
     			if (defaultObj.isShppwsDefaultField()) {
     				String answer = defaultObj.getSegment();
     				segment1ExpiredValues=answer;
     				if(answer.equals("R")) {
     					persona.put("matriz","Valor Vencido Actual EQFX");
         				persona.put("matrizReason", "Valor Vencido Actual EQFX");
         				persona.put("idLN", CV+"");
         				persona.put("msgLN", defaultObj.getShppwsDefaultMessage() + "");
         				break; 
     				}
     			}
     		}
        }
     		
        
        
        //////VALOR Cart. CASTIGADO	    
        Boolean validateDefaultCC = true;
  		OBCriteria<shppec_portpen> matriz2 = OBDal.getInstance().createCriteria(shppec_portpen.class); 
  	    List<shppec_portpen> cartValues = matriz2.list();
  	    String segment2cartValues="-";
          for (shppec_portpen cartValue : cartValues) {
              if (Store_group.equals(cartValue.getAgenCode())) {
              	
              	BigDecimal startValue = cartValue.getValueFrom();
              	BigDecimal endValue = cartValue.getValueUntil();
              	Double valueInitial = startValue.doubleValue();
              	Double valueFinal = endValue.doubleValue();
              	
                  String answer = cartValue.getSegment();
                  if (CC >= valueInitial && CC <= valueFinal) {
                	  validateDefaultCC = false;
                	  segment2cartValues=answer;
                	  if(answer.equals("R")) {
                		  persona.put("matriz","Valor Cartera Castigada EQFX");
     			    	  persona.put("matrizReason","Valor Cartera Castigada EQFX");
     			    	  persona.put("msgLN", cartValue.getMessage()+"");
     			    	  persona.put("idLN",CC+"");
     			    	  break; 
                	  }
                  }
                  //break; 
              }
          }
          
          // DEFAULT
          if(validateDefaultCC) {
          	for (shppec_portpen defaultObj : cartValues) {
       			if (defaultObj.isShppwsDefaultField()) {
       				String answer = defaultObj.getSegment();
       				segment2cartValues=answer;
       				if(answer.equals("R")) {
       					persona.put("matriz","Valor Cartera Castigada EQFX");
           				persona.put("matrizReason", "Valor Cartera Castigada EQFX");
           				persona.put("idLN", CC+"");
           				persona.put("msgLN", defaultObj.getShppwsDefaultMessage() + "");
           				break; 
       				}
       			}
       		}
          }
          
          
          //////Valor Demanda Judicial     
         Boolean validateDefaultDJ = true; 
    	 OBCriteria<Shppec_Lawsuit> matriz3 = OBDal.getInstance().createCriteria(Shppec_Lawsuit.class); 
    	 List<Shppec_Lawsuit> lawsuitValues = matriz3.list();
    	 String segment3lawsuitValues="-";
         for (Shppec_Lawsuit lawsuitValue : lawsuitValues) {
              if (Store_group.equals(lawsuitValue.getAgenCode())) {
                	
                BigDecimal startValue = lawsuitValue.getValueFrom();
                BigDecimal endValue = lawsuitValue.getValueUntil();
                Double valueInitial = startValue.doubleValue();
                Double valueFinal = endValue.doubleValue();
                	
                String answer = lawsuitValue.getSegment();
                  if (DJ >= valueInitial && DJ <= valueFinal) {
                	  validateDefaultDJ = false;
                	  segment3lawsuitValues = answer;
                	  if(answer.equals("R")) {
                		  persona.put("matriz","Valor Demanda Judicial EQFX");
     			    	  persona.put("matrizReason","Valor Demanda Judicial EQFX");
     			    	  persona.put("msgLN", lawsuitValue.getMessage()+"");
     			    	  persona.put("idLN",DJ+"");
     			    	  break; 
                	  }
                  }
                    //break; 
                }
          }
         
         // DEFAULT
         if(validateDefaultDJ) {
         	for (Shppec_Lawsuit defaultObj : lawsuitValues) {
      			if (defaultObj.isShppwsDefaultField()) {
      				String answer = defaultObj.getSegment();
      				segment3lawsuitValues=answer;
      				if(answer.equals("R")) {
      					persona.put("matriz","Valor Demanda Judicial EQFX");
          				persona.put("matrizReason", "Valor Demanda Judicial EQFX");
          				persona.put("idLN", DJ+"");
          				persona.put("msgLN", defaultObj.getShppwsDefaultMessage() + "");
          				break; 
      				}
      			}
      		}
         }
        
        
		persona.put("segment1ExpiredValues", segment1ExpiredValues);
		persona.put("segment2cartValues", segment2cartValues);
		persona.put("segment3lawsuitValues", segment3lawsuitValues);
		 
		
	}
	
	public String validateEquifaxScoreNewClientSP(Map<String, Object> persona, String filterEQ)throws JSONException {
		
		  String newClient_segment=filterEQ;
		  Double newClient_scoreInclusion=(Double) persona.get("EQ_score_inclusion");
		  
		  BigDecimal AgeBigDecimal = (BigDecimal) persona.get("auxAgeBigDecimal");
		  AgeBigDecimal = AgeBigDecimal.setScale(2, RoundingMode.DOWN);
		  Double Age = AgeBigDecimal.doubleValue();
		  
		  String answer="";
		  
		  Boolean validateDefaultNewClient = true;
		  	//////score Cliente Nuevo	     
			OBCriteria<shppee_NewCustomerScore> matriz1= OBDal.getInstance().createCriteria(shppee_NewCustomerScore.class); 
		    List<shppee_NewCustomerScore> CustomerScores = matriz1.list();
			try {
				for (shppee_NewCustomerScore CustomerScore : CustomerScores) {
					if (newClient_segment.equals(CustomerScore.getEquifaxSegment())) {

						BigDecimal scoreInclusionFrom = CustomerScore.getScoreInclusionFrom();
						BigDecimal scoreInclusionEnd = CustomerScore.getScoreInclusionUntil();
						Double valueInitial = scoreInclusionFrom.doubleValue();
						Double valueFinal = scoreInclusionEnd.doubleValue();

						answer = CustomerScore.getENDSegment();
						if (Age >= valueInitial && Age <= valueFinal) {
							validateDefaultNewClient = false;
							persona.put("EQ_scoreClient", answer);
							if(answer.equals("R")) {
		      					persona.put("matriz","Score Cliente nuevo");
		          				persona.put("matrizReason", "Score Cliente nuevo");
		          				persona.put("idLN", "scoreInclusion " + Age);
		          				persona.put("msgLN", CustomerScore.getShppwsDefaultMessage() + "");
		          				break; 
		      				}
						}
						// break;
					}
				}
			} catch (Exception e) {
			}
		  
		     // DEFAULT X
	         if(validateDefaultNewClient) {
	         	for (shppee_NewCustomerScore defaultObj : CustomerScores) {
	      			if (defaultObj.isShppwsDefaultField()) {
	      				answer = defaultObj.getENDSegment();
	      				persona.put("EQ_scoreClient",answer);
	      				if(answer.equals("R")) {
	      					persona.put("matriz","Score Cliente nuevo");
	          				persona.put("matrizReason", "Score Cliente nuevo");
	          				persona.put("idLN", "scoreInclusion " + Age);
	          				persona.put("msgLN", defaultObj.getShppwsDefaultMessage() + "");
	          				break; 
	      				}
	      			}
	      		}
	         }
	         return answer;
	}
	
	
	public void validateEquifaxScoreNewClient(Map<String, Object> persona )throws JSONException {
		
		  String newClient_segment=(String) persona.get("EQ_segmentacion");
		  Double newClient_scoreInclusion=(Double) persona.get("EQ_score_inclusion");
		  
		  BigDecimal AgeBigDecimal = (BigDecimal) persona.get("auxAgeBigDecimal");
		  AgeBigDecimal = AgeBigDecimal.setScale(2, RoundingMode.DOWN);
		  Double Age = AgeBigDecimal.doubleValue();
		  
		  Boolean validateDefaultNewClient = true;
		  	//////score Cliente Nuevo	     
			OBCriteria<shppee_NewCustomerScore> matriz1= OBDal.getInstance().createCriteria(shppee_NewCustomerScore.class); 
		    List<shppee_NewCustomerScore> CustomerScores = matriz1.list();
			try {
				for (shppee_NewCustomerScore CustomerScore : CustomerScores) {
					if (newClient_segment.equals(CustomerScore.getEquifaxSegment())) {

						BigDecimal scoreInclusionFrom = CustomerScore.getScoreInclusionFrom();
						BigDecimal scoreInclusionEnd = CustomerScore.getScoreInclusionUntil();
						Double valueInitial = scoreInclusionFrom.doubleValue();
						Double valueFinal = scoreInclusionEnd.doubleValue();

						String answer = CustomerScore.getENDSegment();
						if (Age >= valueInitial && Age <= valueFinal) {
							validateDefaultNewClient = false;
							persona.put("EQ_scoreClient", answer);
							if(answer.equals("R")) {
		      					persona.put("matriz","Score Cliente nuevo");
		          				persona.put("matrizReason", "Score Cliente nuevo");
		          				persona.put("idLN", "scoreInclusion " + Age);
		          				persona.put("msgLN", CustomerScore.getShppwsDefaultMessage() + "");
		          				break; 
		      				}
						}
						// break;
					}
				}
			} catch (Exception e) {
			}
		  
		     // DEFAULT X
	         if(validateDefaultNewClient) {
	         	for (shppee_NewCustomerScore defaultObj : CustomerScores) {
	      			if (defaultObj.isShppwsDefaultField()) {
	      				String answer = defaultObj.getENDSegment();
	      				persona.put("EQ_scoreClient",answer);
	      				if(answer.equals("R")) {
	      					persona.put("matriz","Score Cliente nuevo");
	          				persona.put("matrizReason", "Score Cliente nuevo");
	          				persona.put("idLN", "scoreInclusion " + Age);
	          				persona.put("msgLN", defaultObj.getShppwsDefaultMessage() + "");
	          				break; 
	      				}
	      			}
	      		}
	         }
	}
	
	public void validateEquifaxQuotas(String filterEQ, Map<String, Object> persona )throws JSONException {
		
		  	//////CUPOS    
			OBCriteria<shppee_Quotas> matriz1= OBDal.getInstance().createCriteria(shppee_Quotas.class); 
		    List<shppee_Quotas> quotasScores = matriz1.list();
		    String codeProduct= (String)persona.get("productcode");
		    String aux="";
		    Boolean validateDefault=true;
	        for (shppee_Quotas quotasScore : quotasScores) {
	        	scsl_Product objProduct = quotasScore.getScslProduct();
	        	String product=objProduct.getValidationCode();
	        	if(codeProduct.equals(product)) {
	        		String storeGroup= (String)persona.get("shopgroup");
	        		if(storeGroup.equals(quotasScore.getAgencyCode())) {
			            if (filterEQ.equals(quotasScore.getENDSegment())) {
			            	validateDefault = false;
			            	Long finalValue = quotasScore.getQuota();
			            	Double valueFinal = finalValue.doubleValue();
			                	persona.put("EQ_quota",valueFinal);
			                Long entrance = quotasScore.getInput();	
			                Double entranceFinal = entrance.doubleValue();
			                	persona.put("EQ_entrance",entranceFinal);
			                String Type_Entrance=quotasScore.getTypeInput();
			                	persona.put("Type_Entrance",Type_Entrance);	
			                Long deadLine = quotasScore.getMaximumTerm();
			                Double deadLineFinal = deadLine.doubleValue();
			                	persona.put("EQ_deadline",deadLineFinal);
			                String filter=quotasScore.getENDSegment();
			                	persona.put("EQ_scoreClient",filter);
			                String checkDelivery=String.valueOf(quotasScore.isSalesdelivery());
			                	persona.put("CheckDelivery",checkDelivery);
			            }
	        		}
	        	}
	        }
	        
	      // DEFAULT X
          if(validateDefault) {
        	for (shppee_Quotas defaultObj : quotasScores) {
				if (defaultObj.isShppwsDefaultField()) {
						Long finalValue = defaultObj.getQuota();
		            	Double valueFinal = finalValue.doubleValue();
		                	persona.put("EQ_quota",valueFinal);
		                Long entrance = defaultObj.getInput();	
		                Double entranceFinal = entrance.doubleValue();
		                	persona.put("EQ_entrance",entranceFinal);
		                String Type_Entrance=defaultObj.getTypeInput();
		                	persona.put("Type_Entrance",Type_Entrance);		
		                Long deadLine = defaultObj.getMaximumTerm();
		                Double deadLineFinal = deadLine.doubleValue();
		                	persona.put("EQ_deadline",deadLineFinal);
		                String filter=defaultObj.getENDSegment();
		                	persona.put("EQ_scoreClient",filter);
		                String msg=defaultObj.getShppwsDefaultMessage();
		                	persona.put("msgLN",msg);
						persona.put("matrizReason", "Matriz de Cupos");
		                String checkDelivery=String.valueOf(defaultObj.isSalesdelivery());
		                	persona.put("CheckDelivery",checkDelivery);

					}
				}
			}
	        
	        persona.put("message","Filtro No 4, Se asigna un cupo al cliente.");
	        persona.put("matriz","Matriz Cupo"+ aux);
	}

	public String newOpportunity(String filter, Map<String, Object> persona, shppws_config accesApi ,boolean statusEquifax)throws JSONException {
		  	//////Se crea una nueva instancia de Oportunidad   
		  Opcrmopportunities objOpportunity = OBProvider.getInstance().get(Opcrmopportunities.class);
		    
		  String newNumber="";
		  String message=".";
		  String Error="";
		  try {
		    String partnerID= (String)persona.get("partnerID") != null ? (String)persona.get("partnerID") : "";
		    BusinessPartner objPartner = OBDal.getInstance().get(BusinessPartner.class, partnerID);
		    if(objPartner != null) {
		    	if(persona.get("CheckDelivery").equals("true")) {
			    	objOpportunity.setShppwsIssalesdelivery(true);
		        }else {
			    	objOpportunity.setShppwsIssalesdelivery(false);
		        }
		    	objOpportunity.setClient(accesApi.getClient());
			    objOpportunity.setOrganization(accesApi.getOrganization());
			    objOpportunity.setActive(accesApi.isActive());
			    objOpportunity.setCreatedBy(accesApi.getCreatedBy());
			    objOpportunity.setUpdatedBy(accesApi.getUpdatedBy());
			    objOpportunity.setBusinessPartner(objPartner);
				    Double amountQuota=(Double)persona.get("EQ_quota");
				    BigDecimal quotaFinal = BigDecimal.valueOf(amountQuota).setScale(2, RoundingMode.HALF_UP);//cupo
			    objOpportunity.setOpportunityAmount(quotaFinal);
			    objOpportunity.setTAXBpartner((String)persona.get("RCcedula"));
			    objOpportunity.setEcsfqEmail((String)persona.get("email"));
			    objOpportunity.setCellphoneBpartner((String)persona.get("CellPhone"));
			    objOpportunity.setEcsfqPhone((String)persona.get("CellPhone"));
			    objOpportunity.setShppwsOpInterface((String)persona.get("interface"));
			    objOpportunity.setShppwsOpChannel((String)persona.get("channel"));
			    OBCriteria<BusinessPartner> queryBusinessPartner = OBDal.getInstance().createCriteria(BusinessPartner.class);
			    queryBusinessPartner.add(Restrictions.eq(BusinessPartner.PROPERTY_SEARCHKEY, (String)persona.get("commercialcode")));
		    	List<BusinessPartner> listObjBusinessPartner = queryBusinessPartner.list();
		    	BusinessPartner objCommercialcode = listObjBusinessPartner.get(0);
		    	if(objCommercialcode != null) {
		    		objOpportunity.setShppwsOpCodecommercial(objCommercialcode);
		    	}
			    objOpportunity.setShppwsOpAgencycode((String)persona.get("agencycode"));
			    objOpportunity.setShppwsOpShopgroup((String)persona.get("shopgroup"));
				    OBCriteria<scsl_Product> queryprod = OBDal.getInstance().createCriteria(scsl_Product.class);
				    queryprod.add(Restrictions.eq(scsl_Product.PROPERTY_VALIDATIONCODE, (String)persona.get("productcode")));
				    scsl_Product product = (scsl_Product)queryprod.uniqueResult();
				    
			    objOpportunity.setShppwsOpProductcode(product);
			    objOpportunity.setShppwsOpEndsegment(filter);
			    objOpportunity.setEcsfqPaymenttype(accesApi.getOPPaymentType());
			    objOpportunity.setOpportunityName(accesApi.getOPName());
			    	Date currentDate = new Date();
			    objOpportunity.setExpectedCloseDate(currentDate);
			    if(filter.equals("R")) {
			    	objOpportunity.setOpportstatus("LOST");//
			    }else {
			    	objOpportunity.setOpportstatus(accesApi.getOPOpportunityStatus());//
			    }
			    if(!statusEquifax) {
			    	objOpportunity.setShppwsDefaultOppSeg(true);
			    }
			    if(typeClientSynergy!=null) {
			    	objOpportunity.setShppwsClienttype(typeClientSynergy); 
			    }
			    objOpportunity.setOpportunityType(accesApi.getOPOpportunityType());
			    objOpportunity.setLeadSource(accesApi.getOPLeadSource());
			    objOpportunity.setShppwsOpDocumentType(accesApi.getOPDocumentType());
			    
			    //Viejo
			    	/*DocumentType DocType = accesApi.getOPDocumentType();
			    	Sequence numberSequence = DocType.getDocumentSequence();
			    newNumber=numberSequence.getPrefix()+""+numberSequence.getNextAssignedNumber();
			    	Long nextValueSequence = numberSequence.getNextAssignedNumber();
			    	Long increment = numberSequence.getIncrementBy();
			    	nextValueSequence = nextValueSequence + increment;
				    numberSequence.setNextAssignedNumber(nextValueSequence);
			    objOpportunity.setShppwsOpDocumentno(newNumber);*/
			    
			    //Nuevo
			    ConnectionProvider conn = new DalConnectionProvider(false);
			    DocumentType DocType = accesApi.getOPDocumentType();
			    org.openbravo.base.secureApp.VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
			    String OpportunityNo = Utility.getDocumentNo(conn.getConnection(), conn, vars, "",Opcrmopportunities.ENTITY_NAME, DocType.getId(), DocType.getId(), false, true);
			    objOpportunity.setShppwsOpDocumentno(OpportunityNo);
			    String ValCedula="NOT";
		    	String ValTel="NOT";
		    	String ValEmail="NOT";
			    if(validateBlackListcheckCedula()) {
			    	ValCedula="YES";
			    }else {
			    	ValCedula="NOT";
			    }
			    if(validateBlackListchecktelefono()) {
			    	ValTel="YES";
			    }else {
			    	ValTel="NOT";
			    }
			    if(validateBlackListcheckemail()) {
			    	ValEmail="YES";
			    }else {
			    	ValEmail="NOT";
			    }
			    objOpportunity.setDescription("Cédulas: "+ValCedula+"\nTeléfonos: "+ValTel+"\nCorreos: "+ValEmail);
			    
			    
			    String City_store_group = (String)persona.get("City_store_group");
			    if(City_store_group != null) {objOpportunity.setShppwsCityStoreGroup(City_store_group);}
			    String Province_store_group = (String)persona.get("Province_store_group");
			    if(Province_store_group != null) {objOpportunity.setShppwsProvinceStoreGroup(Province_store_group);}
			    
			    //New fields
			    Long npcredits=objPartner.getShpctNoPunishedCredits();
			    if(npcredits==null) {npcredits = new Long(0);}
			    objOpportunity.setShppwsOpNoCrdsWritten(npcredits); //N0 creditos castigados
			    Long nccredits=objPartner.getShpctNoCurrentCredits();
			    if(nccredits==null) {nccredits = new Long(0);}
			    objOpportunity.setShppwsOpNoCurCrds(nccredits);//No. Créditos Vigentes
			    Long ncpaid=objPartner.getShpctNoCreditsPaid();
			    if(ncpaid==null) {ncpaid = new Long(0);}
			    objOpportunity.setShppwsOpNoCrdsPaid(ncpaid);//No. Créditos Pagados
			    Long ncexpired=objPartner.getShpctNoCCreditsExpired();
			    if(ncexpired==null) {ncexpired = new Long(0);}
			    objOpportunity.setShppwsOpNoCurOverCrds(ncexpired);//No. Créditos Vigentes Vencidos
			    Long lipaid=objPartner.getShpctLastInstallmentpaid();
			    if(lipaid==null) {lipaid = new Long(0);}
			    objOpportunity.setShppwsOpLastPaidLastcrd(lipaid);//Última cuota pagada del último crédito
			    Long mfpaid=objPartner.getShpctMaximumFeePaid();
			    if(mfpaid==null) {mfpaid = new Long(0);}
			    objOpportunity.setShppwsOpNoMaxFeePaid(mfpaid);//No. Cuota Máxima Pagada
			    Long dlinstallments=objPartner.getShpctDayslateNstallments();
			    if(dlinstallments==null) {dlinstallments = new Long(0);}
			    objOpportunity.setShppwsOpDaysArInstall(dlinstallments);//Días de atraso en cuotas
			    Long ddsdelay=objPartner.getShpctDaysdueSecondDelay();
			    if(ddsdelay==null) {ddsdelay = new Long(0);}
			    objOpportunity.setShppwsOpDays2ndMaxAr(ddsdelay);//Días mora del 2do atraso máximo
			    Long coperations=objPartner.getShpctCurrentOperations();
			    if(coperations==null) {coperations = new Long(0);}
			    objOpportunity.setShppwsOpNoCurOperations(coperations);//No. Operaciones Vigentes
			    BigDecimal irisk=objPartner.getShpctRiskIndex();
			    if(irisk==null) {irisk = new BigDecimal(0);}
			    objOpportunity.setShppwsOpRiskIndex(irisk);//Índice de Riesgo
			    BigDecimal qused=objPartner.getShpctQuotaUsed();
			    if(qused==null) {qused = new BigDecimal(0);}
			    objOpportunity.setShppwsOpQuotaUsed(qused);//Cupo Utilizado
			    
				BigDecimal EQ_entrance = BigDecimal.ZERO;
				try {
					EQ_entrance = BigDecimal.valueOf((Double) (persona.get("EQ_entrance")));
				} catch (Exception e) {}
				String Type_Entrance = "";
				try {
					Type_Entrance =  (String) persona.get("Type_Entrance");
				} catch (Exception e) {}
				objOpportunity.setShppwsEntryAmount(EQ_entrance);
				objOpportunity.setShppwsEntryType(Type_Entrance);
			    
			    Long recover_days_late = (Long) persona.get("recover_days_late");
			    recover_days_late = (recover_days_late == null) ? recover_days_late = new Long(0):recover_days_late;
				BigDecimal recover_amount_pay = (BigDecimal) persona.get("recover_amount_pay");
				recover_amount_pay = (recover_amount_pay == null) ? recover_amount_pay = new BigDecimal(0):recover_amount_pay;
				objOpportunity.setShppwsRecoverDaysLate(recover_days_late);
				objOpportunity.setShppwsRecoverAmountPay(recover_amount_pay);
			    
				if(objCommercialcode != null) {
					OBDal.getInstance().save(objOpportunity);
					OBDal.getInstance().flush();
					persona.put("OP_documentno", objOpportunity.getShppwsOpDocumentno());
					persona.put("OP_record_id", objOpportunity.getId());
			        Error = "Equifax, Se completó la operación y se crea la oportunidad.";
				}else {	
					Error = "No ha sido satisfactoria la carga del comercio.";
				}
		    }else {
		    	Error = "No ha sido satisfactoria la carga del tercero.";
		    }
		    
		  }catch(Exception e){
			  Error = "Error en la Oportunidad "+ e.getMessage();
				OBDal.getInstance().rollbackAndClose();
		  }
		  
		  	try {
			  if(filter.equals("R") && objOpportunity.getBusinessPartner() != null) {
			      //Se crea una nueva instancia de Bitácora   
				  shpctBinnacle objBinnacle = OBProvider.getInstance().get(shpctBinnacle.class);
				  objBinnacle.setClient(accesApi.getClient());
				  objBinnacle.setOrganization(accesApi.getOrganization());
				  objBinnacle.setActive(accesApi.isActive());
				  objBinnacle.setCreatedBy(accesApi.getCreatedBy());
				  objBinnacle.setUpdatedBy(accesApi.getUpdatedBy());
				  objBinnacle.setOpcrmOpportunities(objOpportunity);
				  
				  message+=" op "+ objOpportunity.getShppwsOpDocumentno();
				  objBinnacle.setNameMatrix((String)persona.get("matrizReason"));
					if (persona.containsKey("Identifier") && persona.get("Identifier") != null
							&& !((String) persona.get("Identifier")).isEmpty()) {
						objBinnacle.setMessages((String) persona.get("msgLN") + " : " + persona.get("Identifier"));
					} else {
						objBinnacle.setMessages((String) persona.get("msgLN") + "");
					}
				  objBinnacle.setResults("R");
				  objBinnacle.setComments(persona.get("idLN")+"");
				  OBDal.getInstance().save(objBinnacle);
				  OBDal.getInstance().flush();
				  OBDal.getInstance().getConnection().commit();
				  OBDal.getInstance().refresh(objBinnacle);
				  Error = objBinnacle.getMessages();
			  }else {
				//Se crea una nueva instancia de Bitácora   
				  shpctBinnacle objBinnacle = OBProvider.getInstance().get(shpctBinnacle.class);
				  objBinnacle.setClient(accesApi.getClient());
				  objBinnacle.setOrganization(accesApi.getOrganization());
				  objBinnacle.setActive(accesApi.isActive());
				  objBinnacle.setCreatedBy(accesApi.getCreatedBy());
				  objBinnacle.setUpdatedBy(accesApi.getUpdatedBy());
				  objBinnacle.setOpcrmOpportunities(objOpportunity);
				  String msgSuccesOpp="";
					OBCriteria<shppws_config> queryApi= OBDal.getInstance().createCriteria(shppws_config.class);
				    shppws_config qmsgSuccesOpp = (shppws_config) queryApi.uniqueResult();
				    msgSuccesOpp=qmsgSuccesOpp.getOpportunitySucces();
				  objBinnacle.setMessages(msgSuccesOpp);
				  objBinnacle.setResults("C");
				  objBinnacle.setComments(persona.get("idLN")+"");
				  objBinnacle.setNameMatrix((String) persona.get("matriz"));
				  objBinnacle.setComments((String) persona.get("RCcedula"));
				  OBDal.getInstance().save(objBinnacle);
				  OBDal.getInstance().flush();
				  OBDal.getInstance().getConnection().commit();
				  OBDal.getInstance().refresh(objBinnacle);
				  Error = objBinnacle.getMessages();
			  }
		  }catch(Exception e) {
			  Error = "Hubo un error al generar la Bitácora de la oportunidad " +objOpportunity.getShppwsOpDocumentno() + message +e.getMessage()+e.getMessage();
			  OBDal.getInstance().rollbackAndClose();
		  }
		  return Error;
	}
	
	
	public String newOpportunity_Exception(String filter, Map<String, Object> persona, Ecsce_CustomerException accesApi_exception, shppws_config accesApi, boolean statusEquifax )throws JSONException {
		/////se crea una nueva oportunidad para el proceso de excepcion de clientes
		Opcrmopportunities objOpportunity = OBProvider.getInstance().get(Opcrmopportunities.class);
		String newNumber="";
		String message=".";
		String Error="";
		try {
			String partnerID= (String)persona.get("partnerID") != null ? (String)persona.get("partnerID") : "";
			BusinessPartner objPartner = OBDal.getInstance().get(BusinessPartner.class, partnerID);
		    if(objPartner != null) {
		    	if(persona.get("CheckDelivery").equals("true")) {
			    	objOpportunity.setShppwsIssalesdelivery(true);
		        }else {
			    	objOpportunity.setShppwsIssalesdelivery(false);
		        }
		    	objOpportunity.setClient(accesApi.getClient());
			    objOpportunity.setOrganization(accesApi.getOrganization());
			    objOpportunity.setActive(accesApi.isActive());
			    objOpportunity.setCreatedBy(accesApi.getCreatedBy());
			    objOpportunity.setUpdatedBy(accesApi.getUpdatedBy());
			    objOpportunity.setBusinessPartner(objPartner);
			    BigDecimal quotaException = new BigDecimal(accesApi_exception.getQuota());
				objOpportunity.setOpportunityAmount(quotaException);
			    objOpportunity.setTAXBpartner(accesApi_exception.getTaxID());
			    objOpportunity.setEcsfqEmail((String)persona.get("email"));
			    objOpportunity.setCellphoneBpartner((String)persona.get("CellPhone"));
			    objOpportunity.setEcsfqPhone((String)persona.get("CellPhone"));
			    objOpportunity.setShppwsOpInterface((String)persona.get("interface"));
			    objOpportunity.setShppwsOpChannel((String)persona.get("channel"));
			    OBCriteria<BusinessPartner> queryBusinessPartner = OBDal.getInstance().createCriteria(BusinessPartner.class);
			    queryBusinessPartner.add(Restrictions.eq(BusinessPartner.PROPERTY_SEARCHKEY, (String)persona.get("commercialcode")));
			    List<BusinessPartner> listObjBusinessPartner = queryBusinessPartner.list();
			    BusinessPartner objCommercialcode = listObjBusinessPartner.get(0);
			    if(objCommercialcode != null) {
		    		objOpportunity.setShppwsOpCodecommercial(objCommercialcode);
		    	}
			    objOpportunity.setShppwsOpAgencycode((String)persona.get("agencycode"));
			    objOpportunity.setShppwsOpShopgroup((accesApi_exception.getStoregroup()));
				    OBCriteria<scsl_Product> queryprod = OBDal.getInstance().createCriteria(scsl_Product.class);
				    queryprod.add(Restrictions.eq(scsl_Product.PROPERTY_VALIDATIONCODE, (String)persona.get("productcode")));
				    scsl_Product product = (scsl_Product)queryprod.uniqueResult();
				    
			    objOpportunity.setShppwsOpProductcode(product);
			    objOpportunity.setShppwsOpEndsegment(accesApi_exception.getFinalsegment());
			    objOpportunity.setEcsfqPaymenttype(accesApi.getOPPaymentType());//1
			    objOpportunity.setOpportunityName(accesApi.getOPName());//2
			    	Date currentDate = new Date();
			    objOpportunity.setExpectedCloseDate(currentDate);
			    if(filter.equals("R")) {
			    	objOpportunity.setOpportstatus("LOST");//
			    }else {
			    	objOpportunity.setOpportstatus(accesApi.getOPOpportunityStatus());//3
			    }
			    if(!statusEquifax) {
			    	objOpportunity.setShppwsDefaultOppSeg(true);
			    }
			    if(typeClientSynergy!=null) {
			    	objOpportunity.setShppwsClienttype(typeClientSynergy); 
			    }
			    objOpportunity.setOpportunityType(accesApi.getOPOpportunityType());//4
			    objOpportunity.setLeadSource(accesApi.getOPLeadSource());//5
			    objOpportunity.setShppwsOpDocumentType(accesApi.getOPDocumentType());//6
			    //extra del cupo en seccion datos credito
			    objOpportunity.setShpcfOpCoupon(accesApi_exception.getQuota());
			    boolean CustomerexcepY=true;
			    objOpportunity.setEcsceCustomerexcep(CustomerexcepY);
			    
			    //Viejo
			    	/*DocumentType DocType = accesApi.getOPDocumentType();
			    	Sequence numberSequence = DocType.getDocumentSequence();
			    newNumber=numberSequence.getPrefix()+""+numberSequence.getNextAssignedNumber();
			    	Long nextValueSequence = numberSequence.getNextAssignedNumber();
			    	Long increment = numberSequence.getIncrementBy();
			    	nextValueSequence = nextValueSequence + increment;
				    numberSequence.setNextAssignedNumber(nextValueSequence);
			    objOpportunity.setShppwsOpDocumentno(newNumber);*/
			    
			    //Nuevo
			    ConnectionProvider conn = new DalConnectionProvider(false);
			    DocumentType DocType = accesApi.getOPDocumentType();//7
			    org.openbravo.base.secureApp.VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
			    String OpportunityNo = Utility.getDocumentNo(conn.getConnection(), conn, vars, "",Opcrmopportunities.ENTITY_NAME, DocType.getId(), DocType.getId(), false, true);
			    objOpportunity.setShppwsOpDocumentno(OpportunityNo);
			    objOpportunity.setDescription("Cédulas: NOT"+"\nTeléfonos: NOT"+"\nCorreos: NOT");

			    
			    String City_store_group = (String)persona.get("City_store_group");
			    if(City_store_group != null) {objOpportunity.setShppwsCityStoreGroup(City_store_group);}
			    String Province_store_group = (String)persona.get("Province_store_group");
			    if(Province_store_group != null) {objOpportunity.setShppwsProvinceStoreGroup(Province_store_group);}
			    
			    //New fields
			    Long npcredits=objPartner.getShpctNoPunishedCredits();
			    if(npcredits==null) {npcredits = new Long(0);}
			    objOpportunity.setShppwsOpNoCrdsWritten(npcredits); //N0 creditos castigados
			    Long nccredits=objPartner.getShpctNoCurrentCredits();
			    if(nccredits==null) {nccredits = new Long(0);}
			    objOpportunity.setShppwsOpNoCurCrds(nccredits);//No. Créditos Vigentes
			    Long ncpaid=objPartner.getShpctNoCreditsPaid();
			    if(ncpaid==null) {ncpaid = new Long(0);}
			    objOpportunity.setShppwsOpNoCrdsPaid(ncpaid);//No. Créditos Pagados
			    Long ncexpired=objPartner.getShpctNoCCreditsExpired();
			    if(ncexpired==null) {ncexpired = new Long(0);}
			    objOpportunity.setShppwsOpNoCurOverCrds(ncexpired);//No. Créditos Vigentes Vencidos
			    Long lipaid=objPartner.getShpctLastInstallmentpaid();
			    if(lipaid==null) {lipaid = new Long(0);}
			    objOpportunity.setShppwsOpLastPaidLastcrd(lipaid);//Última cuota pagada del último crédito
			    Long mfpaid=objPartner.getShpctMaximumFeePaid();
			    if(mfpaid==null) {mfpaid = new Long(0);}
			    objOpportunity.setShppwsOpNoMaxFeePaid(mfpaid);//No. Cuota Máxima Pagada
			    Long dlinstallments=objPartner.getShpctDayslateNstallments();
			    if(dlinstallments==null) {dlinstallments = new Long(0);}
			    objOpportunity.setShppwsOpDaysArInstall(dlinstallments);//Días de atraso en cuotas
			    Long ddsdelay=objPartner.getShpctDaysdueSecondDelay();
			    if(ddsdelay==null) {ddsdelay = new Long(0);}
			    objOpportunity.setShppwsOpDays2ndMaxAr(ddsdelay);//Días mora del 2do atraso máximo
			    Long coperations=objPartner.getShpctCurrentOperations();
			    if(coperations==null) {coperations = new Long(0);}
			    objOpportunity.setShppwsOpNoCurOperations(coperations);//No. Operaciones Vigentes
			    BigDecimal irisk=objPartner.getShpctRiskIndex();
			    if(irisk==null) {irisk = new BigDecimal(0);}
			    objOpportunity.setShppwsOpRiskIndex(irisk);//Índice de Riesgo
			    BigDecimal qused=objPartner.getShpctQuotaUsed();
			    if(qused==null) {qused = new BigDecimal(0);}
			    objOpportunity.setShppwsOpQuotaUsed(qused);//Cupo Utilizado
			    
			    BigDecimal EQ_entrance = BigDecimal.ZERO;
				try {
					EQ_entrance = accesApi_exception.getEntry();
				} catch (Exception e) {}
				String Type_Entrance = "";
				try {
					Type_Entrance =  (String) persona.get("Type_Entrance");
				} catch (Exception e) {}
				objOpportunity.setShppwsEntryAmount(EQ_entrance);
				objOpportunity.setShppwsEntryType(Type_Entrance);
			    
			    Long recover_days_late = (Long) persona.get("recover_days_late");
			    recover_days_late = (recover_days_late == null) ? recover_days_late = new Long(0):recover_days_late;
				BigDecimal recover_amount_pay = (BigDecimal) persona.get("recover_amount_pay");
				recover_amount_pay = (recover_amount_pay == null) ? recover_amount_pay = new BigDecimal(0):recover_amount_pay;
				objOpportunity.setShppwsRecoverDaysLate(recover_days_late);
				objOpportunity.setShppwsRecoverAmountPay(recover_amount_pay);
			    
				if(objCommercialcode != null) {
					OBDal.getInstance().save(objOpportunity);
					OBDal.getInstance().flush();
					persona.put("OP_documentno", objOpportunity.getShppwsOpDocumentno());
					persona.put("OP_record_id", objOpportunity.getId());
			        Error = "Equifax, Se completó la operación y se crea la oportunidad.";
				}else {
					Error = "No ha sido satisfactoria la carga del comercio.";
				}
		    }else {
		    	Error = "No ha sido satisfactoria la carga del tercero.";
		    }
		    
		  }catch(Exception e){
			  Error = "Error en la Oportunidad "+ e.getMessage();
				OBDal.getInstance().rollbackAndClose();
		  }
		  
		  try {
			  if(filter.equals("R") && objOpportunity.getBusinessPartner() != null) {
			      //Se crea una nueva instancia de Bitácora   
				  shpctBinnacle objBinnacle = OBProvider.getInstance().get(shpctBinnacle.class);
				  objBinnacle.setClient(accesApi.getClient());
				  objBinnacle.setOrganization(accesApi.getOrganization());
				  objBinnacle.setActive(accesApi.isActive());
				  objBinnacle.setCreatedBy(accesApi.getCreatedBy());
				  objBinnacle.setUpdatedBy(accesApi.getUpdatedBy());
				  objBinnacle.setOpcrmOpportunities(objOpportunity);
				  
				  message+=" op "+ objOpportunity.getShppwsOpDocumentno();
				  objBinnacle.setNameMatrix((String)persona.get("matrizReason"));
					if (persona.containsKey("Identifier") && persona.get("Identifier") != null
							&& !((String) persona.get("Identifier")).isEmpty()) {
						objBinnacle.setMessages((String) persona.get("msgLN") + " : " + persona.get("Identifier"));
					} else {
						objBinnacle.setMessages((String) persona.get("msgLN") + "");
					}
				  objBinnacle.setResults("R");
				  objBinnacle.setComments(persona.get("idLN")+"");
				  OBDal.getInstance().save(objBinnacle);
				  OBDal.getInstance().flush();
				  OBDal.getInstance().getConnection().commit();
				  OBDal.getInstance().refresh(objBinnacle);
				  Error = objBinnacle.getMessages();
			  }
		  }catch(Exception e) {
			  Error = "Hubo un error al generar la Bitácora de la oportunidad " +objOpportunity.getShppwsOpDocumentno() + message +e.getMessage()+e.getMessage();
			  OBDal.getInstance().rollbackAndClose();
		  }
		  return Error;
	}
	
	
	public boolean validateBlackListcheckCedula () {
		boolean checkCedula=false;
		OBCriteria<shppws_config> queryApi= OBDal.getInstance().createCriteria(shppws_config.class);
	    shppws_config accesApi = (shppws_config) queryApi.uniqueResult();
	    checkCedula=accesApi.isCHKLn1Novalidate();
		return checkCedula;
	}
	public boolean validateBlackListchecktelefono () {
		boolean checkTelfono=false;
		OBCriteria<shppws_config> queryApi= OBDal.getInstance().createCriteria(shppws_config.class);
	    shppws_config accesApi = (shppws_config) queryApi.uniqueResult();
	    checkTelfono=accesApi.isCHKLn2Novalidate();
		return checkTelfono;
	}
	public boolean validateBlackListcheckemail () {
		boolean checkEmail=false;
		OBCriteria<shppws_config> queryApi= OBDal.getInstance().createCriteria(shppws_config.class);
	    shppws_config accesApi = (shppws_config) queryApi.uniqueResult();
	    checkEmail=accesApi.isCHKLn3Novalidate();
		return checkEmail;
	}
	public boolean validateSinergy () {
		boolean checksinergy=false;
		OBCriteria<shppws_config> queryApi= OBDal.getInstance().createCriteria(shppws_config.class);
	    shppws_config accesApi = (shppws_config) queryApi.uniqueResult();
	    checksinergy=accesApi.isSynergyIsactivate();
		return checksinergy;
	}
	
	public String getScoreNewClientDefault() {
		String ScoreNewClientDefault="";
		OBCriteria<shppee_NewCustomerScore> queryScoreNewClient = 
		OBDal.getInstance().createCriteria(shppee_NewCustomerScore.class);
		queryScoreNewClient.add(Restrictions.eq(shppee_NewCustomerScore.PROPERTY_SHPPWSDEFAULTEQXSEG, "Y"));
		shppee_NewCustomerScore accesScoreNewClient = (shppee_NewCustomerScore) queryScoreNewClient.uniqueResult();
		ScoreNewClientDefault=accesScoreNewClient.getENDSegment();
		return ScoreNewClientDefault;
	}
	public String getDefaultRiskIndiex () {
		String DefaultRiskIndiex="";
		OBCriteria<shppee_RiskIndex> queryRiskIndex = 
		OBDal.getInstance().createCriteria(shppee_RiskIndex.class);
		queryRiskIndex.add(Restrictions.eq(shppee_RiskIndex.PROPERTY_SHPPWSDEFAULTRSKSEG, true));
		shppee_RiskIndex accesScoreNewClient = (shppee_RiskIndex) queryRiskIndex.uniqueResult();
		DefaultRiskIndiex=accesScoreNewClient.getSegment();
		return DefaultRiskIndiex;
	}
	public String getDefaultRiskIndiexEqError () {
		String DefaultRiskIndiexEQError="";
		OBCriteria<shppee_RiskIndex> queryRiskIndexEqError = 
		OBDal.getInstance().createCriteria(shppee_RiskIndex.class);
		queryRiskIndexEqError.add(Restrictions.eq(shppee_RiskIndex.PROPERTY_SHPPWSDEFAULTRSKSEG, true));
		shppee_RiskIndex accesScoreNewClient = (shppee_RiskIndex) queryRiskIndexEqError.uniqueResult();
		if(accesScoreNewClient==null) {
			DefaultRiskIndiexEQError="R";
		}else {
			DefaultRiskIndiexEQError=accesScoreNewClient.getSegment();
		}
		return DefaultRiskIndiexEQError;
	}
	
	public void validateReferences(String filter, Map<String, Object> persona )throws JSONException {
		String Nacionality = (String)persona.get("RCnationality");
		if(Nacionality==null) {Nacionality = "";}
		if(!Nacionality.equals("ECUATORIANA")) {
			Nacionality = "EXTRANJERA";
		}
		
		OBCriteria<ShpperReferenceMatrix> queryRefrence= OBDal.getInstance().createCriteria(ShpperReferenceMatrix.class);
		queryRefrence.add(Restrictions.eq(ShpperReferenceMatrix.PROPERTY_SHOPGROUP, persona.get("shopgroup")));
		queryRefrence.add(Restrictions.eq(ShpperReferenceMatrix.PROPERTY_ENDSEGMENT, filter));
		queryRefrence.add(Restrictions.eq(ShpperReferenceMatrix.PROPERTY_NATIONALITY, Nacionality));
		List<ShpperReferenceMatrix> listRefrence = queryRefrence.list();
	    
		if(listRefrence.size()>0) {
			try {
		    	ShpperReferenceMatrix objReference = (ShpperReferenceMatrix) queryRefrence.uniqueResult();
		    	persona.put("Ref1", objReference.getReferenceType1());
		    	persona.put("Ref2", objReference.getReferenceType2());
		    	
		    }catch(Exception e) {
		    	persona.put("Ref1", ".");
		    	persona.put("Ref2", ".");
		    }
		}else {
			OBCriteria<ShpperReferenceMatrix> queryRefrenceD= OBDal.getInstance().createCriteria(ShpperReferenceMatrix.class);
			queryRefrenceD.add(Restrictions.eq(ShpperReferenceMatrix.PROPERTY_SHPPWSDEFAULTFIELD, true));
			List<ShpperReferenceMatrix> listRefrenceDef = queryRefrenceD.list();
			try {
		    	ShpperReferenceMatrix objReference = listRefrenceDef.get(0);
		    	persona.put("Ref1", objReference.getReferenceType1());
		    	persona.put("Ref2", objReference.getReferenceType2());
		    }catch(Exception e) {
		    	persona.put("Ref1", ".");
		    	persona.put("Ref2", ".");
		    }
			
		}
	    
		
	}
	
	

	public void validateReferencesException(String filter, Map<String, Object> persona ,Date now)throws JSONException {
		String Nacionality = (String)persona.get("RCnationality");
		if(Nacionality==null) {Nacionality = "";}
		if(!Nacionality.equals("ECUATORIANA")) {
			Nacionality = "EXTRANJERA";
		}
		
		OBCriteria<Ecsce_CustomerException> customerexception = OBDal.getInstance().createCriteria(Ecsce_CustomerException.class);
	    customerexception.add(Restrictions.and(
	    		Restrictions.and(
	    	    		Restrictions.eq(Ecsce_CustomerException.PROPERTY_TAXID, persona.get("Identifier")),
	    	    		Restrictions.eq(Ecsce_CustomerException.PROPERTY_STOREGROUP, persona.get("shopgroup"))
	    	    		    	),
	    	    Restrictions.and(
	    	    		Restrictions.le(Ecsce_CustomerException.PROPERTY_STARTINGDATE, now),
	    	    		Restrictions.ge(Ecsce_CustomerException.PROPERTY_DATEUNTIL, now)
	    	    		    )       // dateUntil >= hoy
	    	    ));
	    customerexception.addOrder(Order.desc(Ecsce_CustomerException.PROPERTY_CREATIONDATE)); // más reciente
	    customerexception.setMaxResults(1);
	    Ecsce_CustomerException cf = (Ecsce_CustomerException) customerexception.uniqueResult();
	    List<Ecsce_CustomerException> listcustomer = customerexception.list();
		
		
	    
	    
	    OBCriteria<ShpperReferenceMatrix> queryRefrence= OBDal.getInstance().createCriteria(ShpperReferenceMatrix.class);
		queryRefrence.add(Restrictions.eq(ShpperReferenceMatrix.PROPERTY_SHOPGROUP, cf.getStoregroup()));
		queryRefrence.add(Restrictions.eq(ShpperReferenceMatrix.PROPERTY_ENDSEGMENT, cf.getFinalsegment()));
		queryRefrence.add(Restrictions.eq(ShpperReferenceMatrix.PROPERTY_NATIONALITY, Nacionality));
		List<ShpperReferenceMatrix> listRefrence = queryRefrence.list();

    	persona.put("SegmentException", cf.getFinalsegment());
	    
	    
		
		
		
		if(listRefrence.size()>0) {
			try {
		    	ShpperReferenceMatrix objReference = (ShpperReferenceMatrix) queryRefrence.uniqueResult();
		    	persona.put("Ref1", objReference.getReferenceType1());
		    	persona.put("Ref2", objReference.getReferenceType2());
		    	
		    }catch(Exception e) {
		    	persona.put("Ref1", ".");
		    	persona.put("Ref2", ".");
		    }
		}else {
			OBCriteria<ShpperReferenceMatrix> queryRefrenceD= OBDal.getInstance().createCriteria(ShpperReferenceMatrix.class);
			queryRefrenceD.add(Restrictions.eq(ShpperReferenceMatrix.PROPERTY_SHPPWSDEFAULTFIELD, true));
			List<ShpperReferenceMatrix> listRefrenceDef = queryRefrenceD.list();
			try {
		    	ShpperReferenceMatrix objReference = listRefrenceDef.get(0);
		    	persona.put("Ref1", objReference.getReferenceType1());
		    	persona.put("Ref2", objReference.getReferenceType2());
		    }catch(Exception e) {
		    	persona.put("Ref1", ".");
		    	persona.put("Ref2", ".");
		    }
			
		}
	    
		
	}
	
	
	
	
	
	
	
}

