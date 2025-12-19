package ec.com.sidesoft.web.services.sales.webservices;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.advpaymentmngt.dao.AdvPaymentMngtDao;
import org.openbravo.advpaymentmngt.process.FIN_AddPayment;
import org.openbravo.advpaymentmngt.process.FIN_TransactionProcess;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.TriggerHandler;
//import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.access.InvoiceLineTax;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.invoice.InvoiceTax;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.accounting.AccountingFact;
import org.openbravo.model.financialmgmt.accounting.Costcenter;
import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentDetail;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentSchedule;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentScheduleDetail;
import org.openbravo.service.web.WebService;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Restrictions;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ec.com.sidesoft.actuaria.special.customization.Scactu_Log;
import ec.com.sidesoft.credit.factory.maintenance.CivilStatus;
import ec.com.sidesoft.credit.simulator.scsl_Creditservices;
import ec.com.sidesoft.happypay.pev.shppev_age;
import ec.com.sidesoft.happypay.web.services.shppws_config;
import ec.com.sidesoft.happypay.web.services.monitor.MonitorManager;
import ec.com.sidesoft.web.services.sales.utils.Swssl_Helper;
import ec.com.sidesoft.web.services.sales.utils.WebServiceDocSequence;
import ec.com.sidesoft.ws.equifax.SweqxEquifax;

import java.util.Base64;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.CertPathValidatorException.Reason;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;

import org.openbravo.database.ConnectionProvider;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.client.kernel.RequestContext;
//import org.openbravo.base.secureApp.VariablesSecureApp;
//import org.hibernate.Session;
//import org.hibernate.Transaction;

public class Reverse_Payment implements WebService{
	private static final long serialVersionUID = 1L;
	private String typeService = "Recover";
	
	public void doGet(String path, HttpServletRequest request, HttpServletResponse response) throws Exception {
		// TODO Auto-generated method stub
	}

	
	public void doPost(String path, HttpServletRequest request, HttpServletResponse response) throws Exception {
		//OBContext.setAdminMode(true);
		//Session session = OBDal.getInstance().getSession();
		//Transaction transaction = session.beginTransaction();
		
		OBCriteria<shppws_config> queryApi= OBDal.getInstance().createCriteria(shppws_config.class);
		shppws_config accesApi = (shppws_config) queryApi.uniqueResult();
		log_records logger = new log_records();
		
		
		JSONObject jsonMonitor = new JSONObject();
		jsonMonitor.put("SHPPWS_SideSoft_Payment", "Service"+0);
		jsonMonitor.put("startSHPPWS_SideSoft_Payment", LocalDateTime.now());
		jsonMonitor.put("typeSHPPWS_SideSoft_Payment", "Pago");
		
		String logMessage="";
		String logStatus="OK";
		
        JSONObject objparentReverse = new JSONObject();
		
		StringBuilder requestBody = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                requestBody.append(line);
            }
        }
        
        Integer code = 104;
        String codeSPagos = "04";
        String error = "";
        OBError message = new OBError();
        
        BigDecimal AmountToRevert = BigDecimal.ZERO;
        Map<String, Object> customMap = new HashMap<>();
        JSONObject requestJSON = new JSONObject(requestBody.toString());
        
        String requestParameters =  requestJSON.toString();
	Scactu_Log log = logger.log_start_register(accesApi, "Reverse_Payment", requestParameters);
        
        String CreditOperationId = requestJSON.has("creditOperationId")? requestJSON.getString("creditOperationId") : null ;
        String amountDecimal  = requestJSON.has("AmountToRevert")? requestJSON.getString("AmountToRevert") : null;
        String Reason = requestJSON.has("Reason")? requestJSON.getString("Reason") : null;
        String IDPag = requestJSON.has("IDPag")? requestJSON.getString("IDPag") : null;
        
		try {
			
		    // Activar el modo de administrador
		    OBContext.setAdminMode(true);

			
			typeService = Swssl_Helper.getTypeService(requestJSON);
	        if(!typeService.equals("Recover")) {
	        	if(typeService.equals("SPagos")) {
	        		error = "NO EXISTE CEDULA CON VALOR ENVIADO";
	        		CreditOperationId = "CreditOperation";
	        		amountDecimal  = Swssl_Helper.getString(requestJSON, "Valor", true); 
	        		Reason = Swssl_Helper.getString(requestJSON,"cedula", true); 
	        		IDPag = Swssl_Helper.getString(requestJSON, "Cod_autorizacion", true);
	        		error = "";
	        	}
	        }

			if (CreditOperationId == null || CreditOperationId.equals("")) {
				error = "INGRESO DE DATOS ERRONEO";
				throw new Exception("verify data imput");
			} else if (amountDecimal == null || amountDecimal.equals("")) {
				error = "INGRESO DE DATOS ERRONEO";
				throw new Exception("verify data imput");
			} else if (Reason == null || Reason.equals("")) {
				error = "INGRESO DE DATOS ERRONEO";
				throw new Exception("verify data imput");
			} else if (IDPag == null || IDPag.equals("")) {
				error = "INGRESO DE DATOS ERRONEO";
				throw new Exception("verify data imput ");
			} else if (getPayment(IDPag).size() > 1) {
				error = "INGRESO DE DATOS ERRONEO";
				throw new Exception("verify duplicated credit ");
			} else if (getPayment(IDPag).size() == 0) {
				code = 103;
				error = "OPERACIÓN DE CREDITO NO EXISTE";
				throw new Exception("verify if exist credit ");
			} else if (getPayment(IDPag).size() == 1) {
				SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
				FIN_Payment objPayment = getPayment(IDPag).get(0);

				Date currentDate = new Date();
				Date paymentDate = objPayment.getCreationDate();

				String currentDateStr = sdf.format(currentDate);
				String paymentDateStr = sdf.format(paymentDate);

				if (!currentDateStr.equals(paymentDateStr)) {// validate today pay
					code = 104;
					error = "INGRESO DE DATOS ERRONEO";
					throw new Exception("Verify date of credit");
				} else {
					BusinessPartner customer = objPayment.getBusinessPartner();
					FIN_Payment CurrentPayment = getOrderedPayment(customer, accesApi);// validate current pay and actually
					if (CurrentPayment.getId()==null||CurrentPayment.getId().equals("")||!(CurrentPayment.getId().equals(objPayment.getId()))) {
						code = 104;
						error = "INGRESO DE DATOS ERRONEO";
						throw new Exception("Order to reverse");
					}
				}
			}

			if (error.equals("")) {
				ConnectionProvider conn = new DalConnectionProvider(false);
				AmountToRevert = new BigDecimal(amountDecimal);
				FIN_Payment payment = getPayment(IDPag).get(0);
				if (payment.getStatus().equals("SWSSL_REV")) {
					code = 103;
					error = "IDPag " + IDPag + "OPERACIÓN DE CRÉDITO NO EXISTE";
				} else {
					int precision = payment.getCurrency().getPricePrecision().intValue();

					List<Invoice> oginalSalesInvoice = getoriginalInvoice(payment);
					List<Invoice> serviceSalesInvoice = getServiceSalesInvoice(payment);

					message = updateStatusPaymentPlan(oginalSalesInvoice, IDPag);
					if("Error".equals(message.getType())) {
						throw new Exception("Error updateStatusPaymentPlan "+message.getMessage());
					}
					
					refinancing(oginalSalesInvoice, IDPag);
					
					List<FIN_Payment> listRefANT = getRefANT(payment, accesApi);// if exist refANT
					if (listRefANT.size() > 0) {
						for (FIN_Payment objRefANT : listRefANT) {

							if (objRefANT.getSwsslCreditOpe() == payment) {// campo -> No. Cobro Principal - Cruce
								objRefANT.setSwsslCreditOpe(null);
								OBDal.getInstance().save(objRefANT);
								OBDal.getInstance().flush();
							} else if (objRefANT.getSwsslPaymentOrigin() == payment) {// campo -> Cobro Origen
								if (objRefANT.getStatus() != null && !(objRefANT.getStatus().equals("SWSSL_REV"))) {
									message = reverse(conn, precision, objRefANT, Reason);
									if ("Error".equals(message.getType())) {
										error = "INGRESO DE DATOS ERRONEO";
										throw new Exception("Error reverse "+message.getMessage());
									} else {
										objRefANT.setStatus("SWSSL_REV");
										objRefANT.setSwsslRevreason(Reason);
										OBDal.getInstance().save(objRefANT);
										OBDal.getInstance().flush();
									}
								}
							}
						}
					}

					message = reverse(conn, precision, payment, Reason);//PAYMENT
					if ("Error".equals(message.getType())) {
						error = "INGRESO DE DATOS ERRONEO";
						throw new Exception("Error reverse "+message.getMessage());
					} else {
						code = 0;
						codeSPagos = "01";
						error = "Transacción Exitosa";
						payment.setStatus("SWSSL_REV");
						payment.setSwsslRevreason(Reason);
						OBDal.getInstance().save(payment);
						OBDal.getInstance().flush();
					}

					if (serviceSalesInvoice.size() > 0) {
						List<Invoice> creditNotes = creditNotes(conn, accesApi, serviceSalesInvoice);
						OBDal.getInstance().getConnection().commit();
						Boolean c_invoice_post = c_invoice_post(creditNotes);
						if (c_invoice_post) {
							error = "Transacción Exitosa";// "Successful operation c_invoice_post";
						} else {
							error = "INGRESO DE DATOS ERRONEO";
							code = 104;
							throw new Exception("cant execute c_invoice_post");
						}
						crossing(conn, accesApi, serviceSalesInvoice, creditNotes, payment, customMap);
					}

                     //Final
					if (oginalSalesInvoice.size() > 0) {
						message = updateIndicatorsInvoice(oginalSalesInvoice, accesApi);
						if("Error".equals(message.getType())) {
							throw new Exception("Error oginalSalesInvoice "+message.getMessage());
						}
					}
				}
			}
			
			logMessage="Reversado";
			logStatus="OK";
			//OBDal.getInstance().commitAndClose();
			OBDal.getInstance().getConnection().commit();
		} catch (Exception e) {
			logMessage="Error: "+e.getMessage();
			logStatus="ERROR";
			//OBDal.getInstance().rollbackAndClose();
			OBDal.getInstance().getConnection().rollback();
		} finally {
		    // Asegúrate de desactivar el modo de administrador independientemente de si hubo una excepción
		    OBContext.setAdminMode(false);
		}
        
		if(typeService.equals("Recover")) {
			objparentReverse.put("code",code);
	        objparentReverse.put("Message",error);
	        objparentReverse.put("IDPag",IDPag);
	        objparentReverse.put("crossDocID",customMap.get("crossDocID"));
		}else if(typeService.equals("SPagos")) {
			objparentReverse.put("Codigo_respuesta",codeSPagos);
			objparentReverse.put("Codigo_autorizacion",IDPag);
			if(codeSPagos.equals("01")) {
				error = "REVERSO REGISTRADO";
			}else {
				error = "NO EXISTE CEDULA CON VALOR ENVIADO";
			}
	        objparentReverse.put("Mensaje",error);
	        objparentReverse.put("crossDocID",customMap.get("crossDocID"));
		}
        
        
        //Finalmente devuelvo
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		String json = objparentReverse.toString();
		PrintWriter writer = response.getWriter();
		writer.write(json);
		writer.close();
		
		String requestUrl = request.getRequestURL().toString();
        logger.log_end_register(log, requestUrl, "Sistema Recaudador", json, logStatus, "OUT", "Sistema Recaudador", "Reversos", IDPag, logMessage);
        
        jsonMonitor.put("endSHPPWS_SideSoft_Payment", LocalDateTime.now());
        jsonMonitor.put("Message", error);
	if(code != null && code == 0) {
		jsonMonitor.put("statusSHPPWS_SideSoft_Payment", "200");
	}else {
		jsonMonitor.put("statusSHPPWS_SideSoft_Payment", "500");
	}
	jsonMonitor.put("Identifier", IDPag );
	jsonMonitor.put("TypeOfMonitor", "Reverso");
	MonitorManager newMonitor = new MonitorManager();
	newMonitor.sendMonitorData(jsonMonitor, accesApi, true, null);
	try {
		OBDal.getInstance().getConnection().close();
	}catch(Exception e) {
		 e.printStackTrace();
	}
    }

	
	public void doDelete(String path, HttpServletRequest request, HttpServletResponse response) throws Exception {
		// TODO Auto-generated method stub
		
	}

	
	public void doPut(String path, HttpServletRequest request, HttpServletResponse response) throws Exception {
		// TODO Auto-generated method stub
		
	}
	
	public List<FIN_Payment> getPayment(String documentNO){
    	OBCriteria<FIN_Payment> queryFIN_Payment= OBDal.getInstance().createCriteria(FIN_Payment.class);
  		queryFIN_Payment.add(Restrictions.eq(FIN_Payment.PROPERTY_DOCUMENTNO, documentNO));
  		List<FIN_Payment> listFIN_Payment = queryFIN_Payment.list();
  		
  		return listFIN_Payment;
	}
	
	public FIN_Payment getOrderedPayment(BusinessPartner customer, shppws_config accesApi){//Cobros y anticipos originales
		FIN_Payment objFIN_Payment = new FIN_Payment();
		OBCriteria<FIN_Payment> queryFIN_Payment= OBDal.getInstance().createCriteria(FIN_Payment.class);
  		queryFIN_Payment.add(Restrictions.eq(FIN_Payment.PROPERTY_BUSINESSPARTNER, customer));
  		queryFIN_Payment.add(Restrictions.ne(FIN_Payment.PROPERTY_STATUS, "SWSSL_REV"));
  		queryFIN_Payment.add(Restrictions.isNull(FIN_Payment.PROPERTY_SWSSLPAYMENTORIGIN));//Cobro original
  		queryFIN_Payment.add(Restrictions.isNull(FIN_Payment.PROPERTY_SWSSLCREDITOPE));//Cobro Principal - Cruce
  		
  		Disjunction disjunction = Restrictions.disjunction(); //OR
	    disjunction.add(Restrictions.eq(FIN_Payment.PROPERTY_DOCUMENTTYPE, accesApi.getSwsslDoctypeAdvance()));//Tipo ant
	    disjunction.add(Restrictions.eq(FIN_Payment.PROPERTY_DOCUMENTTYPE, accesApi.getSwsslCDoctypePay()));//Tipo cobro
	    queryFIN_Payment.add(disjunction); //ADD QUERY OR

  		List<FIN_Payment> listFIN_Payment = queryFIN_Payment.list();
  		if(listFIN_Payment.size()>0) {
  			Collections.sort(listFIN_Payment, (e1, e2) -> e2.getCreationDate().compareTo(e1.getCreationDate()));
  	  		objFIN_Payment = listFIN_Payment.get(0);
  		}
  		return objFIN_Payment;
	}
	
	public List<FIN_Payment> getRefANT(FIN_Payment payment, shppws_config accesApi){
	    OBCriteria<FIN_Payment> queryFIN_Payment = OBDal.getInstance().createCriteria(FIN_Payment.class);
	    queryFIN_Payment.add(Restrictions.eq(FIN_Payment.PROPERTY_DOCUMENTTYPE, accesApi.getSwsslDoctypeAdvance()));
	    queryFIN_Payment.add(Restrictions.ne(FIN_Payment.PROPERTY_STATUS, "SWSSL_REV"));
	    
	    Disjunction disjunction = Restrictions.disjunction(); //OR
	    disjunction.add(Restrictions.eq(FIN_Payment.PROPERTY_SWSSLPAYMENTORIGIN, payment));//origin payment
	    disjunction.add(Restrictions.eq(FIN_Payment.PROPERTY_SWSSLCREDITOPE, payment));//Cobro Principal - Cruce
	    queryFIN_Payment.add(disjunction); //ADD QUERY OR

	    List<FIN_Payment> listRefANT = queryFIN_Payment.list();
	    return listRefANT;
	}

	private List<Invoice> getServiceSalesInvoice(FIN_Payment payment) {
		List<Invoice> listInvoices = new ArrayList<Invoice>();
		try {
			OBCriteria<FIN_PaymentDetail> queryFIN_PaymentDetail = OBDal.getInstance()
					.createCriteria(FIN_PaymentDetail.class);
			queryFIN_PaymentDetail.add(Restrictions.eq(FIN_PaymentDetail.PROPERTY_FINPAYMENT, payment));
			List<FIN_PaymentDetail> listFIN_PaymentDetail = queryFIN_PaymentDetail.list();
			String[] paymentDeatailIds = new String[listFIN_PaymentDetail.size()];
			int index = 0;
			for (FIN_PaymentDetail objPaymentDetail : listFIN_PaymentDetail) {
				paymentDeatailIds[index] = objPaymentDetail.getId();
				index++;
			}

			OBCriteria<FIN_PaymentScheduleDetail> queryFIN_PaymentScheduleDetail = OBDal.getInstance()
					.createCriteria(FIN_PaymentScheduleDetail.class);
			queryFIN_PaymentScheduleDetail
					.add(Restrictions.in(FIN_PaymentScheduleDetail.PROPERTY_PAYMENTDETAILS + ".id", paymentDeatailIds));
			List<FIN_PaymentScheduleDetail> listFIN_PaymentScheduleDetail = queryFIN_PaymentScheduleDetail.list();

			for (FIN_PaymentScheduleDetail paymentDetail : listFIN_PaymentScheduleDetail) {
				Invoice invoice  = paymentDetail.getInvoicePaymentSchedule().getInvoice();
				if(invoice.isSwsslInvoiceAS()) {
					listInvoices.add(invoice);
				}
			}

			Collections.sort(listInvoices, (e1, e2) -> e2.getCreationDate().compareTo(e1.getCreationDate()));

			return listInvoices;
		} catch (Exception e) {
			return listInvoices;
		}
	}
	
	private List<Invoice> getoriginalInvoice(FIN_Payment payment) {
		List<Invoice> listInvoices = new ArrayList<Invoice>();
		try {
			OBCriteria<FIN_PaymentDetail> queryFIN_PaymentDetail = OBDal.getInstance()
					.createCriteria(FIN_PaymentDetail.class);
			queryFIN_PaymentDetail.add(Restrictions.eq(FIN_PaymentDetail.PROPERTY_FINPAYMENT, payment));
			List<FIN_PaymentDetail> listFIN_PaymentDetail = queryFIN_PaymentDetail.list();
			String[] paymentDeatailIds = new String[listFIN_PaymentDetail.size()];
			int index = 0;
			for (FIN_PaymentDetail objPaymentDetail : listFIN_PaymentDetail) {
				paymentDeatailIds[index] = objPaymentDetail.getId();
				index++;
			}

			OBCriteria<FIN_PaymentScheduleDetail> queryFIN_PaymentScheduleDetail = OBDal.getInstance()
					.createCriteria(FIN_PaymentScheduleDetail.class);
			queryFIN_PaymentScheduleDetail
					.add(Restrictions.in(FIN_PaymentScheduleDetail.PROPERTY_PAYMENTDETAILS + ".id", paymentDeatailIds));
			List<FIN_PaymentScheduleDetail> listFIN_PaymentScheduleDetail = queryFIN_PaymentScheduleDetail.list();

			for (FIN_PaymentScheduleDetail paymentDetail : listFIN_PaymentScheduleDetail) {
				try {
					Invoice invoice = paymentDetail.getInvoicePaymentSchedule().getInvoice();
					if (invoice.isSwsslInvoiceAS() == false) {
						listInvoices.add(invoice);
					}
				} catch (Exception e) {listInvoices.add(payment.getShpicNodocumeennt());
				}
			}
			
			if(listInvoices.size() <= 0 && payment.getShpcfAdditionalvalue() != null && payment.getShpcfAdditionalvalue().compareTo(BigDecimal.ZERO) > 0) {
				Invoice invoice = payment.getShpicNodocumeennt();
				listInvoices.add(invoice);
			}
			
			Collections.sort(listInvoices, (e1, e2) -> e2.getCreationDate().compareTo(e1.getCreationDate()));
			
			return listInvoices;
		} catch (Exception e) {
			return listInvoices;
		}
	}
	
	private OBError reverse(ConnectionProvider conn, int precision, FIN_Payment payment, String reverseReason) throws Exception {
		OBError message = new OBError();
		message.setType("Error");
		try {
			OBCriteria<FIN_FinaccTransaction> qTransaction = OBDal.getInstance()
					.createCriteria(FIN_FinaccTransaction.class);
			qTransaction.add(Restrictions.eq(FIN_FinaccTransaction.PROPERTY_FINPAYMENT, payment));
			for (FIN_FinaccTransaction transaction : qTransaction.list()) {
				String posted = transaction.getPosted();
				if(posted != null && posted.equals("Y")) {
					transaction.setPosted("N");
					OBDal.getInstance().save(transaction);
				}
				FIN_TransactionProcess.doTransactionProcess("R", transaction);
				OBDal.getInstance().remove(transaction);
				OBDal.getInstance().flush();
			}

			Table table = OBDal.getInstance().get(Table.class, "D1A97202E832470285C9B1EB026D54E2"); // FIN_Payment

			OBCriteria<AccountingFact> qAccounting = OBDal.getInstance().createCriteria(AccountingFact.class);
			qAccounting.add(Restrictions.eq(AccountingFact.PROPERTY_TABLE, table));
			qAccounting.add(Restrictions.eq(AccountingFact.PROPERTY_RECORDID, payment.getId()));
			for (AccountingFact accounting : qAccounting.list()) {
				OBDal.getInstance().remove(accounting);
				OBDal.getInstance().flush();
			}

			payment.setPosted("N");
			// payment.setShwsReverseDate(reverseDate);
			// payment.setShwsReverseTicketNo("");
			// payment.setShwsReverseReason(reverseReason);
			OBDal.getInstance().save(payment);
			OBDal.getInstance().flush();

			org.openbravo.base.secureApp.VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
			message = FIN_AddPayment.processPayment(vars, conn, "R", payment);

			return message;
		} catch (Exception e) {
			message.setMessage("Payment cant reactivate "+e.getMessage());
			return message;
		}
	}
	
	private List<Invoice> creditNotes(ConnectionProvider conn, shppws_config accesApi, List<Invoice> invoices) throws Exception {
		    List<Invoice> creditNotes = new ArrayList<Invoice>();
		    for (Invoice invoice : invoices) {
		      Invoice creditNote = OBProvider.getInstance().get(Invoice.class);
		      creditNote.setSalesTransaction(true);
		      creditNote.setClient(invoice.getClient());
		      creditNote.setOrganization(invoice.getOrganization());
		      creditNote.setInvoiceDate(invoice.getInvoiceDate());
		      creditNote.setAccountingDate(invoice.getAccountingDate());
		      creditNote.setDocumentStatus("CO");
		      creditNote.setDocumentAction("CO");
		      creditNote.setAPRMProcessinvoice("RE");
		      creditNote.setProcessed(false);
		      creditNote.setProcessNow(false);
		      creditNote.setDescription(".");
		      String taxId = invoice.getBusinessPartner().getTaxID();
		      creditNote.setSpincoTaxid(taxId);
		      
		      Organization orgCostc = OBDal.getInstance().get(Organization.class,invoice.getOrganization().getId());

		      org.openbravo.base.secureApp.VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
		      DocumentType documentType = accesApi.getSwsslRevdocumenttype();
		      String invoiceNo = Utility.getDocumentNo(conn.getConnection(), conn, vars, "",
		          Invoice.ENTITY_NAME, documentType.getId(), documentType.getId(), false, true);

		      creditNote.setDocumentType(documentType);
		      creditNote.setTransactionDocument(documentType);
		      creditNote.setDocumentNo(invoiceNo);
		      creditNote.setPaymentMethod(accesApi.getSwsslRevpaymentmethod());
		      creditNote.setBusinessPartner(invoice.getBusinessPartner());
		      creditNote.setPartnerAddress(invoice.getPartnerAddress());
		      creditNote.setCurrency(invoice.getCurrency());
		      creditNote.setPaymentTerms(invoice.getPaymentTerms());
		      creditNote.setPriceList(invoice.getPriceList());
		      creditNote.setCostcenter(invoice.getCostcenter());
		      creditNote.setOutstandingAmount(invoice.getOutstandingAmount());
		      creditNote.setShpicNodocument(invoice.getShpicNodocument());
		      creditNote.setShpicPaymentchannel(invoice.getShpicPaymentchannel());
		      creditNote.setShpicNoquoteess(invoice.getShpicNoquoteess());
		      
		      creditNote.setScnrIsrefInv(true);
		      creditNote.setScnrInvoice(invoice);
		      
		      OBDal.getInstance().save(creditNote);
		      OBDal.getInstance().flush();

		      for (InvoiceLine invoiceLine : invoice.getInvoiceLineList()) {
		        InvoiceLine line = OBProvider.getInstance().get(InvoiceLine.class);
		        line.setInvoice(creditNote);
		        line.setClient(invoiceLine.getClient());
		        line.setOrganization(invoiceLine.getOrganization());
		        line.setLineNo(invoiceLine.getLineNo());
		        line.setProduct(invoiceLine.getProduct());
		        line.setSprliIdentifier(invoiceLine.getSprliIdentifier());
		        line.setUOM(invoiceLine.getUOM());
		        line.setInvoicedQuantity(invoiceLine.getInvoicedQuantity());
		        line.setUnitPrice(invoiceLine.getUnitPrice());
		        line.setStandardPrice(invoiceLine.getStandardPrice());
		        line.setLineNetAmount(invoiceLine.getLineNetAmount());
		        line.setListPrice(invoiceLine.getListPrice());
		        line.setTaxAmount(invoiceLine.getTaxAmount());
		        line.setTax(invoiceLine.getTax());
		        line.setSsbodDiscountRate(invoiceLine.getSsbodDiscountRate());
		        line.setSseedDiscount(invoiceLine.getSseedDiscount());
		        line.setCostcenter(invoiceLine.getCostcenter());
		        line.setStDimension(invoiceLine.getStDimension());
		        OBDal.getInstance().save(line);
		        OBDal.getInstance().flush();
		      }
				
		      creditNotes.add(creditNote);
		    }

		    return creditNotes;
		  }
	
	public Boolean c_invoice_post(List<Invoice> listcreditNote) {
		try {
			for (Invoice objcreditNote : listcreditNote) {
				final List<Object> parameters = new ArrayList<Object>();
				parameters.add(null);
				parameters.add(objcreditNote.getId());
				final String procedureName = "c_invoice_post";
				CallStoredProcedure.getInstance().call(procedureName, parameters, null, true, false);
				OBDal.getInstance().refresh(objcreditNote);
			    OBDal.getInstance().save(objcreditNote);
			    OBDal.getInstance().flush();
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	 
	private void crossing(ConnectionProvider conn, shppws_config accesApi, List<Invoice> invoices, List<Invoice> creditNotes, FIN_Payment payment, Map<String, Object> customMap) throws Exception {
		    FIN_PaymentMethod paymentMethod = accesApi.getSwsslCrosspaymentmethod();
		    BusinessPartner customer = new BusinessPartner();
		    Costcenter costCenter = new Costcenter();
		    Date paymentDate = new Date();
		    Organization org = new Organization();
		    FIN_PaymentDetail fp = new FIN_PaymentDetail();
		    FIN_PaymentDetail fp2 = new FIN_PaymentDetail();
		    List<FIN_PaymentScheduleDetail> details = new ArrayList<FIN_PaymentScheduleDetail>();
		    HashMap<String, BigDecimal> paidAmounts = new HashMap<String, BigDecimal>();
		    AdvPaymentMngtDao dao = new AdvPaymentMngtDao();
		    
		    for (Invoice invoice : invoices) {
			      List<FIN_PaymentScheduleDetail> scheduleDetails = dao.getInvoicePendingScheduledPaymentDetails(invoice);
			      BigDecimal paidAmount = invoice.getOutstandingAmount();
			      for (int j = 0; j < scheduleDetails.size(); j++) {
			        FIN_PaymentScheduleDetail item = scheduleDetails.get(j);
			        if (paidAmount.compareTo(item.getAmount()) <= 0) {
			          paidAmounts.put(item.getId(), paidAmount);
			          paidAmount = BigDecimal.ZERO;
			        } else {
			          paidAmounts.put(item.getId(), item.getAmount());
			          paidAmount = paidAmount.subtract(item.getAmount());
			        }

			        if (paidAmount.compareTo(BigDecimal.ZERO) == 0) {
			          j = scheduleDetails.size();
			        }
			        details.add(item);
			        fp = item.getPaymentDetails();
			      }
			      customer = invoice.getBusinessPartner();
			      costCenter = invoice.getCostcenter();
			      paymentDate = invoice.getCreationDate();
			      org = invoice.getOrganization();
			    }
		    
		    for (Invoice creditNote : creditNotes) {
				
			      List<FIN_PaymentScheduleDetail> scheduleDetails = dao.getInvoicePendingScheduledPaymentDetails(creditNote);
			      BigDecimal paidAmount = creditNote.getOutstandingAmount();
			      for (int j = 0; j < scheduleDetails.size(); j++) {
			        FIN_PaymentScheduleDetail item = scheduleDetails.get(j);
			        if (paidAmount.compareTo(item.getAmount()) >= 0) {
			          paidAmounts.put(item.getId(), paidAmount);
			          paidAmount = BigDecimal.ZERO;
			        } else {
			          paidAmounts.put(item.getId(), item.getAmount());
			          paidAmount = paidAmount.add(item.getAmount().abs());
			        }

			        if (paidAmount.compareTo(BigDecimal.ZERO) == 0) {
			          j = scheduleDetails.size();
			        }
			        details.add(item);
			        fp2 = item.getPaymentDetails();
			      }
			      customer = creditNote.getBusinessPartner();
			      costCenter = creditNote.getCostcenter();
			      paymentDate = creditNote.getCreationDate();
			      org = creditNote.getOrganization();
			      
			    }
		    
		    boolean isWriteOff = false;

		    org.openbravo.base.secureApp.VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();

		    DocumentType documentType = accesApi.getSwsslCrossdocumenttype();

		    String paymentNo = Utility.getDocumentNo(conn.getConnection(), conn, vars, "",
		        FIN_Payment.ENTITY_NAME, documentType.getId(), documentType.getId(), false, true);

		    FIN_Payment newPayment = FIN_AddPayment.savePayment(null, true, documentType, paymentNo,
		        customer, paymentMethod, accesApi.getSwsslCrossfinAccount(), "0", paymentDate, org,
		        null, details, paidAmounts, isWriteOff, false);

		    //newPayment.setSspacCBpartner(customer);
		    //newPayment.setBusinessPartner(customer);
		    newPayment.setCostCenter(costCenter);
		    newPayment.setPaymentMethod(paymentMethod);
		    newPayment.setAccount(accesApi.getSwsslCrossfinAccount());
		    newPayment.setCurrency(accesApi.getSwsslCrossfinAccount().getCurrency());
		    newPayment.setStatus(payment.getStatus());
		    newPayment.setSwsslRevreason(payment.getSwsslRevreason());
		    newPayment.setShpicNodocumeennt(payment.getShpicNodocumeennt());
		    newPayment.setShpicNoquotescharg(payment.getShpicNoquotescharg());
		    newPayment.setShpicPaymentchannelcharg(payment.getShpicPaymentchannelcharg());
		    newPayment.setShpicProductcharg(payment.getShpicProductcharg());
		    newPayment.setShpicByscsprcharg(payment.getShpicByscsprcharg());
		    newPayment.setSwsslCreditOpe(payment);
		    newPayment.setSwsslImportChargedTotal(payment.getSwsslImportChargedTotal());
		    
		    
		    OBDal.getInstance().save(newPayment);
		    OBDal.getInstance().flush();
		    //OBDal.getInstance().getConnection().commit();
		    customMap.put("crossDocID", newPayment.getId());
		    
		    String strDocAction = "P";
		    OBError message = null;
		    //message = FIN_AddPayment.processPayment(vars, conn, strDocAction, newPayment);
		    message = FIN_AddPayment.processPayment(vars, conn, strDocAction, newPayment, "TRANSACTION");
		    //if (configPaymentMethod.isGenerateTransaction()) {
		     // message = FIN_AddPayment.processPayment(vars, conn, strDocAction, newPayment);
		   // } else {
		     // message = FIN_AddPayment.processPayment(vars, conn, strDocAction, newPayment, "TRANSACTION");
		    //}

		    if ("Error".equals(message.getType())) {
		      throw new OBException(message.getMessage());
		    }

		    OBDal.getInstance().refresh(newPayment);

    }

	public OBError updateStatusPaymentPlan(List<Invoice> oginalSalesInvoice, String IDPag) {
		OBError message = new OBError();
		message.setType("Success");
		message.setMessage("");
		try {
		    String shppsFeeStatus = "PDG";
			StringBuilder whereClause = new StringBuilder();
	        whereClause.append(" WHERE ");
	        whereClause.append(" TRIM('" + IDPag + "') IN (" + "id" + ", " + "documentNo" + ")");
	        final OBQuery<FIN_Payment> payment = (OBQuery<FIN_Payment>)OBDal.getInstance().createQuery((Class)FIN_Payment.class, whereClause.toString());
	        payment.uniqueResult();
	        if (payment.list().size() == 0) {
	            throw new OBException("Cobro no existe ");
	        }
	        FIN_Payment py = (FIN_Payment) payment.uniqueResult();
		    for (FIN_PaymentDetail dt : py.getFINPaymentDetailList()) {
			    for (FIN_PaymentScheduleDetail line : dt.getFINPaymentScheduleDetailList()) {
					try {
						if (line.getInvoicePaymentSchedule().getShppsExpensesCollection().compareTo(BigDecimal.ZERO) == 0) {
							line.getInvoicePaymentSchedule().setShppsFeeStatus(shppsFeeStatus);
							OBDal.getInstance().save(line);
							OBDal.getInstance().flush();
						}
					} catch (Exception e) {}
				}
		    }
		}catch(Exception e) {
			message.setType("Error");
			message.setMessage("updateStatusPaymentPlan" + e.getMessage());
		}
		return message;
	}
	
	public OBError updateIndicatorsInvoice(List<Invoice> oginalSalesInvoice, shppws_config accesApi) {
		OBError message = new OBError();
		message.setType("Success");
		message.setMessage("");
		try {
			for(Invoice invoice : oginalSalesInvoice) {
				
				//Payment Plan
				OBCriteria<FIN_PaymentSchedule> queryInvoiceLines = OBDal.getInstance().createCriteria(FIN_PaymentSchedule.class);
				queryInvoiceLines.add(Restrictions.eq(FIN_PaymentSchedule.PROPERTY_INVOICE, invoice));
				List<FIN_PaymentSchedule> listLinesInvoice = queryInvoiceLines.list();
				Collections.sort(listLinesInvoice, (e1, e2) -> e1.getSwsslNroCuota().compareTo(e2.getSwsslNroCuota()));
				Date EM_Shpic_Proxdues = new Date();//fecha prox cuota
				//Date EM_Shpic_Duedate = new Date();//fecha de vencimiento prox cuota
				BigDecimal EM_Shpic_Mostover_Install = BigDecimal.ZERO;//cuota mas vencida
				long EM_Shpic_Lastduespaid = new Long(0);
				FIN_PaymentSchedule auxLine = null;
				
				for(FIN_PaymentSchedule line : listLinesInvoice) {//CUOTA ACTUAL
					Date LastPaymentDate = line.getLastPaymentDate();//último pago
					if (LastPaymentDate == null) {//verifica ultima cuota pagada
						if(auxLine == null) {//CUOTA ANTERIOR
							auxLine=listLinesInvoice.get(0);
						}
						EM_Shpic_Proxdues = line.getDueDate();
						EM_Shpic_Mostover_Install = EM_Shpic_Mostover_Install.add(line.getScslTotalquota());
						EM_Shpic_Lastduespaid = auxLine.getSwsslNroCuota();
						if(auxLine.getLastPaymentDate()==null) {
							EM_Shpic_Lastduespaid = new Long(0);
						}
						break;
					}else {
						auxLine = line;
					}
				}
				
				//Anticipos
				final StringBuffer hqlString = new StringBuffer();
			    hqlString.append(" as p");
			    hqlString.append(" WHERE generatedCredit<>0 ");
			    hqlString.append(" AND usedCredit<>generatedCredit ");
			    hqlString.append(" AND businessPartner.id=:partnerId ");
			    hqlString.append(" AND shpicNodocumeennt.id=:invoiceid ");
			    hqlString.append(" ORDER BY paymentDate,documentNo ");
			    final OBQuery<FIN_Payment> advPayments = (OBQuery<FIN_Payment>)OBDal.getInstance().createQuery((Class)FIN_Payment.class, hqlString.toString());
			    advPayments.setNamedParameter("partnerId", invoice.getBusinessPartner().getId());
			    advPayments.setNamedParameter("invoiceid", invoice.getId());
				List<FIN_Payment> listANT = advPayments.list();
				if(listANT.size()>0) {
					BigDecimal amountANT = BigDecimal.ZERO;
					for(FIN_Payment objANT : listANT) {
						amountANT = amountANT.add(objANT.getAmount());
					}
					invoice.setShpicAdvancevalue(amountANT);
				}else {
					invoice.setShpicAdvancevalue(BigDecimal.ZERO);
				}
				
				//Actualizando
				invoice.setShpicProxdues(EM_Shpic_Proxdues);
				invoice.setShpicMostoverInstall(EM_Shpic_Mostover_Install);
				invoice.setShpicLastduespaid(EM_Shpic_Lastduespaid);
				invoice.setShpicPaymentUpdate(true);
				invoice.setShpicExpirationUpdate(true);
				OBDal.getInstance().save(invoice);
				OBDal.getInstance().flush();
			}
		}catch(Exception e) {
			message.setType("Error");
			message.setMessage(e.getMessage());
		}
		return message;
	}
	
	public void refinancing(List<Invoice> oginalSalesInvoice, String IDPag) {
			for(Invoice invoice : oginalSalesInvoice) {
				OBCriteria<FIN_PaymentSchedule> queryFIN_PaymentScheduleDetail = OBDal.getInstance()
						.createCriteria(FIN_PaymentSchedule.class);
				queryFIN_PaymentScheduleDetail
						.add(Restrictions.eq(FIN_PaymentSchedule.PROPERTY_INVOICE + ".id", invoice.getId()));
				List<FIN_PaymentSchedule> listFIN_PaymentScheduleDetail = queryFIN_PaymentScheduleDetail.list();

				for (FIN_PaymentSchedule paymentDetail : listFIN_PaymentScheduleDetail) {
					if(paymentDetail.isSwsslExpcollCalculate()) {
						invoice.setSwsslIsrefinancing(Boolean.valueOf(true));
						OBDal.getInstance().save((Object)invoice);
				        OBDal.getInstance().flush();
				        OBDal.getInstance().refresh((Object)invoice);
		                final List<Object> parameters = new ArrayList<Object>();
		                parameters.add(invoice.getId());
		                parameters.add("REV");
		                parameters.add(IDPag);
		                final String procedureName = "swssl_update_invoice";
		                CallStoredProcedure.getInstance().call("swssl_update_invoice", parameters, null, true, false);
					}
				}
			}
	}
	
	

}
