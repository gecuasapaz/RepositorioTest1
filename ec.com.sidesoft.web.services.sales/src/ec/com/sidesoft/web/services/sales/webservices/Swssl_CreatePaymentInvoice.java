package ec.com.sidesoft.web.services.sales.webservices;

import org.codehaus.jettison.json.JSONException;
import org.hibernate.type.StringType;
import org.openbravo.advpaymentmngt.process.FIN_PaymentProcess;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.invoice.InvoiceLine;
import java.math.RoundingMode;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.Query;
import org.hibernate.Session;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.common.enterprise.DocumentType;
import java.util.Iterator;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.sql.Connection;
import java.sql.PreparedStatement;
//import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentSchedule;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.advpaymentmngt.process.FIN_AddPayment;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.financialmgmt.gl.GLItem;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentDetail;
import org.codehaus.jettison.json.JSONArray;
import java.util.HashMap;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentScheduleDetail;
import java.util.ArrayList;
import org.openbravo.advpaymentmngt.dao.AdvPaymentMngtDao;
import org.openbravo.model.ad.access.InvoiceLineTax;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.model.common.enterprise.Organization;
import java.util.List;
import org.openbravo.dal.service.OBQuery;

import ec.com.sidesoft.credit.simulator.AdditionalServicesInv;
import ec.com.sidesoft.credit.simulator.scsl_Creditservices;
import ec.com.sidesoft.credit.simulator.scsl_Product;
import ec.com.sidesoft.happypay.credit.factory.ShpcfCollectionQuota;
import ec.com.sidesoft.happypay.credit.factory.ShpcfPaymentChannel;
import ec.com.sidesoft.happypay.web.services.shppws_config;
import ec.com.sidesoft.happypay.web.services.shppws_monitor;
import ec.com.sidesoft.happypay.web.services.monitor.MonitorManager;

import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.base.exception.OBException;
import java.math.BigDecimal;
import java.io.Writer;
import org.openbravo.database.ConnectionProvider;
import org.apache.commons.lang.StringUtils;
import org.openbravo.dal.service.OBDal;
import org.openbravo.base.provider.OBProvider;
import ec.com.sidesoft.actuaria.special.customization.Scactu_Log;
import java.io.InputStream;

import ec.com.sidesoft.web.services.sales.utils.CustomSalesInvoice;
import ec.com.sidesoft.web.services.sales.utils.CustomSalesInvoice2;
import ec.com.sidesoft.web.services.sales.utils.Swssl_Helper;
import ec.com.sidesoft.web.services.sales.utils.WebServiceDocSequence;

import java.util.Date;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.TriggerHandler;
import org.openbravo.service.db.DalConnectionProvider;
import org.codehaus.jettison.json.JSONObject;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;
import org.openbravo.service.web.WebService;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.invoice.InvoiceTax;


public class Swssl_CreatePaymentInvoice implements WebService
{
    private final Logger logger;
    int cuotasPagadas = 0;
    private String typeService = "Recover";
    String channelId="";
    String channelName="";
    Boolean isAdditionalAmount = false;
    BigDecimal additionalAmount = BigDecimal.ZERO;
    public Swssl_CreatePaymentInvoice() {
        this.logger = Logger.getLogger((Class)Swssl_CreatePaymentInvoice.class);
    }
    
    public void doPost(final String path, final HttpServletRequest request, final HttpServletResponse response) throws Exception {
        JSONObject json = new JSONObject();
        String logId = null;
        String Identifier ="";
        String Auxpaymenchannel ="";
        JSONObject jsonMonitor = new JSONObject();
	jsonMonitor.put("SHPPWS_SideSoft_Payment", "Service"+0);
	jsonMonitor.put("startSHPPWS_SideSoft_Payment", LocalDateTime.now());
	jsonMonitor.put("typeSHPPWS_SideSoft_Payment", "Pago");
        
        final ConnectionProvider conn = (ConnectionProvider)new DalConnectionProvider(false);
        Label_0508: {
            try {
                OBContext.setAdminMode(true);
                this.logger.info((Object)"Begin Swssl_CreatePaymentInvoice doPost");
                final Date loggerDate = new Date();
                JSONObject body = Swssl_Helper.readAllIntoJSONObject((InputStream)request.getInputStream());
                final String paymenchannel = Swssl_Helper.getString(body, "paymenchannel", true);
                Auxpaymenchannel = "Cobro - "+paymenchannel;
                this.logger.info((Object)"Json enviado");
                this.logger.info((Object)loggerDate.toString());
                this.logger.info((Object)body.toString());
                final Scactu_Log log = (Scactu_Log)OBProvider.getInstance().get((Class)Scactu_Log.class);
                log.setEndpoint("createPaymentInvoice");
                log.setJsonRequest(body.toString());
                OBDal.getInstance().save((Object)log);
                OBDal.getInstance().flush();
                OBDal.getInstance().getConnection().commit();
                logId = log.getId();
                
                typeService = Swssl_Helper.getTypeService(body);
                if(!typeService.equals("Recover")) {
                	body = Swssl_Helper.getStructureJSON(body, typeService);
                	Identifier = body.has("cedula")? body.getString("cedula"):""; 
                }
                jsonMonitor.put("body", body);
                
                this.create(conn, body);
                json = Swssl_Helper.getResponse(body, typeService);
                log.setJsonResponse(json.toString());
                log.setRecordID(body.getString("paymentId"));
                log.setReferenceNo(body.getString("paymentNo"));
                log.setResult("OK");
                log.setShppwsProcess("Cobro - "+body.getString("paymentNo"));
                OBDal.getInstance().save((Object)log);
                OBDal.getInstance().flush();
                //OBDal.getInstance().commitAndClose();
                OBDal.getInstance().getConnection().commit();
                json.remove("data");
                jsonMonitor.put("Message", json);
            } catch (Exception e) {
                OBDal.getInstance().rollbackAndClose();
                final String message = Swssl_Helper.getErrorMessage(this.logger, e);
                this.logger.error((Object)message);
                final String[] data = StringUtils.split(e.getMessage().toString(), ",");
                final Scactu_Log log2 = (Scactu_Log)OBDal.getInstance().get((Class)Scactu_Log.class, (Object)logId);      
                if (log2 != null) {
                    log2.setResult("ERROR");
                    log2.setError(message);
                    log2.setShppwsProcess(Auxpaymenchannel.length() > 60 ? Auxpaymenchannel.substring(0, 60): Auxpaymenchannel);
            		Date date = new Date();
                    log2.setShppwsStartTime(date);
                    log2.setShppwsEndTime(date);
                    OBDal.getInstance().save((Object)log2);
                    OBDal.getInstance().flush();
                    OBDal.getInstance().getConnection().commit();
                }	
                json = Swssl_Helper.getErrorResponse(message, typeService);
                jsonMonitor.put("Message", json);
                if(typeService.equals("Recover")) {
                	if (data.length > 1) {
                        json.put("code", (Object)data[0]);
                        json.put("Message", (Object)data[1]);
                    }
                }else if(typeService.equals("SPagos")) {
                	json.put("cedula", Identifier);
                	if (data.length > 1) {
                		//json.put("code", (Object)data[0]);
                        json.put("Mensaje", (Object)data[1]);
                    }
                }
                
                break Label_0508;
           } finally { 
    		    OBContext.setAdminMode(false);
                this.logger.info((Object)"Finish Swssl_CreatePaymentInvoice doPost");
            }
            //OBContext.restorePreviousMode();
            //this.logger.info((Object)"Finish Swssl_CreatePaymentInvoice doPost");
        }
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        final Writer w = response.getWriter();
        w.write(json.toString());
        w.close();
	jsonMonitor.put("endSHPPWS_SideSoft_Payment", LocalDateTime.now());
        String OP_documentno = jsonMonitor.has("body") && jsonMonitor.getJSONObject("body").has("paymentId")? jsonMonitor.getJSONObject("body").getString("paymentId") : "";
	if(OP_documentno != null && !OP_documentno.equals("")) {
		jsonMonitor.put("statusSHPPWS_SideSoft_Payment", "200");
	}else {
		jsonMonitor.put("statusSHPPWS_SideSoft_Payment", "500");
	}
	jsonMonitor.put("Identifier", OP_documentno != null && !OP_documentno.equals("")?  OP_documentno: jsonMonitor.getJSONObject("body").getString("creditOperationId") );
	jsonMonitor.put("TypeOfMonitor", "Pagos");
	MonitorManager newMonitor = new MonitorManager();
	newMonitor.sendMonitorData(jsonMonitor, null, true, null);
	try {
	        conn.getConnection().close();
	        conn.destroy();
        }catch (Exception e2) {
            e2.printStackTrace();
        }
		
    }
    
    public void doGet(final String path, final HttpServletRequest request, final HttpServletResponse response) throws Exception {
    }
    
    public void doPut(final String path, final HttpServletRequest request, final HttpServletResponse response) throws Exception {
    }
    
    public void doDelete(final String path, final HttpServletRequest request, final HttpServletResponse response) throws Exception {
    }
    
    private void create(final ConnectionProvider conn, final JSONObject body) throws Exception {
        
        StringBuilder whereClause = new StringBuilder();
        final Date paymentDate = new Date();
        this.logger.info((Object)paymentDate);
        final BigDecimal amountToPay = Swssl_Helper.getBigDecimal(body, "amountToPay", true);
        if (amountToPay.compareTo(BigDecimal.ZERO) == 0) {
            throw new OBException("104, INGRESO DE DATOS ERRONEO");
        }
        final String invoiceId = Swssl_Helper.getString(body, "creditOperationId", true);
        final String company = Swssl_Helper.getString(body, "Company", true);
        whereClause = new StringBuilder();
        whereClause.append(" WHERE ");
        whereClause.append(" TRIM('" + invoiceId + "') IN (" + "id" + ", " + "documentNo" + ")");
        final OBQuery<Invoice> qInvoice = (OBQuery<Invoice>)OBDal.getInstance().createQuery((Class)Invoice.class, whereClause.toString());
        if (qInvoice.list().size() == 0) {
            throw new OBException("103,Operacion de Credito no existe ");
        }
        final Invoice invoice = qInvoice.list().get(0);
        if (invoice.getShpicProduct() == null) {
            throw new OBException("104,Invoice [" + invoiceId + "] not have Credit Product");
        }
        whereClause = new StringBuilder();
        whereClause.append(" WHERE ");
        whereClause.append("active = 'Y' ");
        whereClause.append(" AND organization.id = '" + invoice.getOrganization().getId() + "'");
        final OBQuery<shppws_config> qConfig = (OBQuery<shppws_config>)OBDal.getInstance().createQuery((Class)shppws_config.class, whereClause.toString());
        if (qConfig.list().size() == 0) {
            throw new OBException("104,Configuration not found for organization [" + invoice.getOrganization().getName() + "]");
        }
        final shppws_config config = qConfig.list().get(0);
        if (config.getSwsslPaymentmethod() == null) {
            throw new OBException("104,Payment Method for Invoice A/S not found for organization [" + invoice.getOrganization().getName() + "]");
        }
        final OBQuery<ShpcfCollectionQuota> qConfigLines = (OBQuery<ShpcfCollectionQuota>)OBDal.getInstance().createQuery((Class)ShpcfCollectionQuota.class, whereClause.toString());
        if (qConfigLines.list().size() == 0) {
            throw new OBException("104,Configuration not found for organization [" + invoice.getOrganization().getName() + "]");
        }
        /*final ShpcfCollectionQuota configlines = qConfigLines.list().get(0);
        if (configlines.getPaymentaccount() == null) {
            throw new OBException("104,Financial Account for Payment not found for organization [" + invoice.getOrganization().getName() + "]");
        }*/
        if (config.getSwsslCPaymenttermInv() == null) {
            throw new OBException("104,Payment Term for Invoice A/S not found for organization [" + invoice.getOrganization().getName() + "]");
        }
        if (config.getSwsslCostCenterInv() == null) {
            throw new OBException("104,Cost Center for Invoice A/S not found for organization [" + invoice.getOrganization().getName() + "]");
        }
        whereClause = new StringBuilder();
        whereClause.append(" WHERE ");
        whereClause.append("active = 'Y' ");
        whereClause.append(" AND scslProduct.id = '" + invoice.getShpicProduct().getId() + "'");
        final OBQuery<scsl_Creditservices> qConfigLine = (OBQuery<scsl_Creditservices>)OBDal.getInstance().createQuery((Class)scsl_Creditservices.class, whereClause.toString());
        if (qConfigLine.list().size() == 0) {
            throw new OBException("104,Configuration not found for organization [" + invoice.getShpicProduct().getId() + "]");
        }
        final List<scsl_Creditservices> configLine = (List<scsl_Creditservices>)qConfigLine.list();
        final int precision = invoice.getCurrency().getPricePrecision().intValue();
        this.paymentIn(conn, body, invoice.getOrganization(), configLine, invoice, precision, paymentDate, config,company);
    }
    
    public List<ShpcfPaymentChannel> getChannelPayment(String psdSet) {
        StringBuffer where = new StringBuffer();
        where.append(" as psd");
        where.append(" where psd." + ShpcfPaymentChannel.PROPERTY_NAME + " in (:name)");
        OBQuery<ShpcfPaymentChannel> channel = OBDal.getInstance()
            .createQuery(ShpcfPaymentChannel.class, where.toString());
        channel.setNamedParameter("name", psdSet);
        return channel.list();
      }
    
    private JSONObject paymentIn(final ConnectionProvider conn, final JSONObject body, final Organization org, final List<scsl_Creditservices> configLine, final Invoice invoice, final int precision, final Date paymentDate, final shppws_config config, final String company) throws Exception {
        final JSONObject jsonAttServ = new JSONObject();
        JSONArray jsonArrayAddServ = new JSONArray();
        final String paymenchannel = Swssl_Helper.getString(body, "paymenchannel", true);
		ShpcfPaymentChannel channelpayment = null;
		int indice=0;

		try {
			// Intentar buscar el canal de pago
			OBQuery<ShpcfPaymentChannel> query = OBDal.getInstance().createQuery(ShpcfPaymentChannel.class,
					"as c where c.name = :name");
			query.setNamedParameter("name", paymenchannel);
			query.setMaxResult(1);
			List<ShpcfPaymentChannel> results = query.list();

			if (!results.isEmpty()) {
				channelpayment = results.get(0);
			}

		} catch (Exception e) {
			logger.debug("Error al obtener canal de pago", e);
		}
		
		if (channelpayment == null) {
			throw new OBException("104, Channel Payment is not found");
		}
	    
		//Se busca la configuracion en cobros de cuotas
        OBCriteria<ShpcfCollectionQuota> queryCollectionQuota = OBDal.getInstance().createCriteria(ShpcfCollectionQuota.class);
        queryCollectionQuota.add(Restrictions.eq(ShpcfCollectionQuota.PROPERTY_SHPCFPAYMENTCHANNEL,channelpayment));
        queryCollectionQuota.add(Restrictions.eq(ShpcfCollectionQuota.PROPERTY_ACTIVE,true));
        // Aquí armamos el OR
        Disjunction orCondition = Restrictions.disjunction();
        orCondition.add(Restrictions.eq(ShpcfCollectionQuota.PROPERTY_ISPAYMENTTRANSACTION, true));
        orCondition.add(Restrictions.eq(ShpcfCollectionQuota.PROPERTY_ISADVANCETRANSACTION, true));
        // Lo agregamos a la query
        queryCollectionQuota.add(orCondition);
        
        queryCollectionQuota.setMaxResults(1);
		 List<ShpcfCollectionQuota> listOp =  queryCollectionQuota.list();
        if(listOp.size()==0||listOp.isEmpty()) {
            throw new OBException("104, Channel Payment is not found");
        }
        ShpcfCollectionQuota quota = listOp.get(0);

        FIN_FinancialAccount paymentAccount = quota.getPaymentaccount();
        FIN_FinancialAccount paymentAccountAnt = quota.getAdvanceaccount();
        
        //Valor Adicional
        isAdditionalAmount = quota.getAdditionalvalue() != null && quota.getAdditionalvalue().compareTo(BigDecimal.ZERO) > 0 ? true: false;
        additionalAmount = quota.getAdditionalvalue();
        GLItem glitemAdditionalAmt = quota.getGLItem();
        Boolean generateInvoiceAdditionalAmt = quota.isSueinvoice();
        
        BigDecimal amountToPayOrigin = Swssl_Helper.getBigDecimal(body, "amountToPay", true);
        BigDecimal amountToPay = Swssl_Helper.getBigDecimal(body, "amountToPay", true);
        
        if(amountToPay.compareTo(additionalAmount) < 0) {
        	isAdditionalAmount = false;
        }
        
        if(isAdditionalAmount) {
        	body.put("amountToPay", amountToPay.subtract(additionalAmount));
        	amountToPay = Swssl_Helper.getBigDecimal(body, "amountToPay", true);
			if (!generateInvoiceAdditionalAmt && glitemAdditionalAmt == null) {
				throw new OBException("104, GL Item is required to register additional amount");
        	}
        }
        
        BigDecimal amountexp = BigDecimal.ZERO;
        final boolean isWriteOff = false;
        final org.openbravo.base.secureApp.VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
        StringBuilder whereClause = new StringBuilder();
        whereClause.append(" WHERE ");
        whereClause.append(" TRIM('" + invoice.getBusinessPartner().getId() + "') IN (" + "id" + ", " + "taxID" + ")");
        final OBQuery<BusinessPartner> qCustomer = (OBQuery<BusinessPartner>)OBDal.getInstance().createQuery((Class)BusinessPartner.class, whereClause.toString());
        if (qCustomer.list().size() == 0) {
            throw new OBException("104,customer for paymen not found");
        }
        final BusinessPartner customer = qCustomer.list().get(0);
        PreparedStatement sqlQuery = null;
        ResultSet rs = null;
        final String sql = "select sum(coalesce(EM_Shpps_Expenses_Collection,0)) as amountexp from fin_payment_schedule where c_invoice_id = ? and outstandingamt > 0";
        sqlQuery = new DalConnectionProvider(false).getPreparedStatement(sql);
        sqlQuery.setString(1, invoice.getId());
        sqlQuery.execute();
        rs = sqlQuery.getResultSet();
        while (rs.next()) {
            amountexp = ((rs.getBigDecimal("amountexp") == null) ? BigDecimal.ZERO : rs.getBigDecimal("amountexp"));
        }
        BigDecimal financivaluedues = invoice.getShpicFinancivaluedues();
        if (financivaluedues == null) {
            financivaluedues = BigDecimal.ZERO;
        }
        final AdvPaymentMngtDao dao = new AdvPaymentMngtDao();
        List<FIN_PaymentScheduleDetail> scheduleDetails = (List<FIN_PaymentScheduleDetail>)dao.getInvoicePendingScheduledPaymentDetails(invoice);
        if (scheduleDetails.size() == 0) {
            throw new OBException("102,Sin creditos vigentes");
        }
        final List<FIN_PaymentScheduleDetail> details = new ArrayList<FIN_PaymentScheduleDetail>();
        final HashMap<String, BigDecimal> paidAmounts = new HashMap<String, BigDecimal>();
        BigDecimal paidAmount = new BigDecimal(amountToPay.toString());
        BigDecimal asistAmount = BigDecimal.ZERO;
        BigDecimal servAmount = BigDecimal.ZERO;
        BigDecimal asistAmountinit = BigDecimal.ZERO;
        BigDecimal servAmountinit = BigDecimal.ZERO;
        BigDecimal amountToPayment = BigDecimal.ZERO;
        BigDecimal expensesCollectioninit = BigDecimal.ZERO;
        BigDecimal expensesCollection = BigDecimal.ZERO;
        BigDecimal additionalServicesinit = BigDecimal.ZERO;
        BigDecimal addservice = BigDecimal.ZERO;
        boolean isExpensesCollection = false;


        final BigDecimal advancedAmount = paidAmount;
        BigDecimal generateCredit = BigDecimal.ZERO;
        Long noquota = 0L;
        Long noquotaPayment = 0L;
        String ShpicNoquotescharg = "";
        final List<FIN_Payment> advps = this.getAdvPayments(customer.getId(), invoice.getId());
        final JSONArray selectedCreditLines = new JSONArray();
        for (final FIN_Payment psd : advps) {
            final JSONObject advPayment = new JSONObject();
            final BigDecimal avalAmount = psd.getGeneratedCredit().subtract(psd.getUsedCredit());
            final BigDecimal availableAmount =avalAmount.setScale(2, RoundingMode.HALF_UP);
            advPayment.put("id", (Object)psd.getId());
            advPayment.put("paymentAmount", (Object)availableAmount);
            selectedCreditLines.put((Object)advPayment);
            paidAmount = paidAmount.add(availableAmount);
            generateCredit = generateCredit.add(availableAmount);
            generateCredit = generateCredit.setScale(2, RoundingMode.HALF_UP);
        }

        FIN_Payment newPaymentAdv = null;
        FIN_Payment newPaymentAdvAdditionalVal = null;
        WebServiceDocSequence classUpdateSeq = new WebServiceDocSequence();
        for (int j = 0; j < scheduleDetails.size(); ++j) {
            final FIN_PaymentScheduleDetail item = scheduleDetails.get(j);
            asistAmountinit = ((item.getInvoicePaymentSchedule().getShppsAttendance() == null) ? BigDecimal.ZERO : item.getInvoicePaymentSchedule().getShppsAttendance());
            servAmountinit = ((item.getInvoicePaymentSchedule().getShppsService() == null) ? BigDecimal.ZERO : item.getInvoicePaymentSchedule().getShppsService());
            expensesCollectioninit = ((item.getInvoicePaymentSchedule().getShppsExpensesCollection() == null) ? BigDecimal.ZERO : item.getInvoicePaymentSchedule().getShppsExpensesCollection());
            additionalServicesinit = item.getInvoicePaymentSchedule().getScslAddservices();
            
            if (item.getInvoicePaymentSchedule().isSwsslExpcollCalculate()) {
            	isExpensesCollection = true;
            }
            	            
            if (paidAmount.compareTo(item.getAmount().add(asistAmountinit.add(servAmountinit.add(expensesCollectioninit.add(additionalServicesinit))))) < 0) {
                final List<FIN_PaymentScheduleDetail> selectedPaymentDetails = new ArrayList<FIN_PaymentScheduleDetail>();
                final HashMap<String, BigDecimal> selectedPaymentDetailsAmounts = new HashMap<String, BigDecimal>();
                final DocumentType documentType = config.getSwsslDoctypeAdvance();
                final String strAction = "PRD";
                if (amountToPayment.compareTo(BigDecimal.ZERO) == 0) {
                	//BigDecimal round_paid_ammount = generateCredit;
                	generateCredit=generateCredit.setScale(2, RoundingMode.HALF_UP);
                	//round_paid_ammount = round_paid_ammount.setScale(2, RoundingMode.HALF_UP);
                    paidAmount = paidAmount.subtract(generateCredit);
                }
                final String paymentNo = Utility.getDocumentNo(conn.getConnection(), conn, vars, "", "FIN_Payment", documentType.getId(), documentType.getId(), false, true);
                final FIN_Payment newPayment = FIN_AddPayment.savePayment((FIN_Payment)null, (boolean)invoice.isSalesTransaction(), documentType, paymentNo, customer, config.getSwsslPaymentmethodAnt(), quota.getAdvanceaccount(), paidAmount.toString(), paymentDate, org, (String)null, (List)selectedPaymentDetails, (HashMap)selectedPaymentDetailsAmounts, isWriteOff, false);
                newPayment.setShpicNodocumeennt(invoice);
                newPayment.setShpicNoquotescharg(ShpicNoquotescharg);
                newPayment.setShpicPaymentchannelcharg(channelpayment);
                //newPayment.setAccount(paymentAccountAnt);
                newPayment.setShpicProductcharg(invoice.getShpicProduct());
                newPayment.setShpicByscsprcharg(invoice.getShpicByscspr());
                newPayment.setSwsslImportChargedTotal(amountToPayOrigin);
                newPayment.setSwsslCompany(company);
                if(ShpicNoquotescharg.equals("")) {
                	ShpicNoquotescharg="*0";
                }
                	indice = ShpicNoquotescharg.lastIndexOf('*'); // Encuentra la última aparición de '*'
                    String ultimoNumero = ShpicNoquotescharg.substring(indice + 1); // Obtiene todo después del '*'
                    Long totalquota =invoice.getShpicNodues();
                    long UltimaCuotaLong = Long.parseLong(ultimoNumero);
                        if(UltimaCuotaLong<totalquota) {
                        	if(UltimaCuotaLong==0) {
                        		newPayment.setShpicNoquotescharg("1");
                        	}else {
    	                	UltimaCuotaLong+=1;
    	                	String StringUltimaCuota=String.valueOf(UltimaCuotaLong);
    	                	newPayment.setShpicNoquotescharg(StringUltimaCuota);
                        	}
                        }else {
                        	newPayment.setShpicNoquotescharg(ultimoNumero);
                        }
                    
                
                
                String UltimaCuota=String.valueOf(invoice.getShpicLastduespaid());
            	long cuota=invoice.getShpicLastduespaid();
                    if(invoice.getShpicLastduespaid()<invoice.getShpicNodues()) {
	                	cuota+=1;
	                	String StringUltimaCuota=String.valueOf(cuota);
	                	newPayment.setShpicNoquotescharg(StringUltimaCuota);
                    }else {
                    	newPayment.setShpicNoquotescharg(UltimaCuota);
                    }
                
                
                if (amountToPayment.compareTo(BigDecimal.ZERO) > 0) {
                	newPaymentAdv = newPayment;
                }
                
                final OBError message = FIN_AddPayment.processPayment(vars, conn, (strAction.equals("PRP") || strAction.equals("PPP")) ? "P" : "D", newPayment, (String)null);
                if ("Error".equals(message.getType())) {
                    throw new OBException(message.getMessage());
                }
                final BigDecimal shpicAdvancevalue = (invoice.getShpicAdvancevalue() == null) ? BigDecimal.ZERO : invoice.getShpicAdvancevalue();
                invoice.setShpicAdvancevalue(shpicAdvancevalue.add(paidAmount));
                invoice.setShpicPaymentUpdate(true);
                OBDal.getInstance().save((Object)invoice);
                OBDal.getInstance().flush();
                OBDal.getInstance().refresh((Object)invoice);
                newPaymentAdvAdditionalVal = newPayment;
                body.put("paymentId", (Object)newPayment.getId());
                body.put("paymentNo", (Object)newPayment.getDocumentNo());
                paidAmount = BigDecimal.ZERO;
                j = scheduleDetails.size();
            }
            else {
            	paidAmounts.put(item.getId(), item.getAmount());
                paidAmount = paidAmount.subtract(item.getAmount().add(asistAmountinit.add(servAmountinit.add(expensesCollectioninit.add(additionalServicesinit)))));
                noquota = ((item.getInvoicePaymentSchedule().getSwsslNroCuota() == null) ? 0L : item.getInvoicePaymentSchedule().getSwsslNroCuota());
                details.add(item);
                cuotasPagadas++;
                asistAmount = asistAmount.add(asistAmountinit);
                servAmount = servAmount.add(servAmountinit);
                expensesCollection = expensesCollection.add(expensesCollectioninit);
                amountToPayment = amountToPayment.add(item.getAmount());
                Long cuotaPayment = item.getInvoicePaymentSchedule().getSwsslNroCuota();
                ShpicNoquotescharg = String.valueOf(ShpicNoquotescharg) + '*' + noquota.toString();
                addservice=additionalServicesinit.add(additionalServicesinit);
            }
            if (paidAmount.compareTo(BigDecimal.ZERO) == 0) {
                j = scheduleDetails.size();
            }
            
            if(item.getInvoicePaymentSchedule() != null && item.getInvoicePaymentSchedule().getScslAdditionalServicesInvList().size() > 0) {
            	for(AdditionalServicesInv objAddServInv : item.getInvoicePaymentSchedule().getScslAdditionalServicesInvList()) {
            		JSONObject addServ = new JSONObject();
            		addServ.put("typeAddService", objAddServInv.getAdditionalservice().getSwsslTypeprod());
            		addServ.put("idProdAddService", objAddServInv.getAdditionalservice().getProduct().getId());
            		addServ.put("amt", objAddServInv.getSearchKey());
            		addServ.put("id", objAddServInv.getAdditionalservice().getId());
            		addServ.put("addservice", addservice);
            		addServ.put("qty", indice);
            		jsonArrayAddServ.put(addServ);
            	}
            }
            
        }
        jsonAttServ.put("ATT", (Object)asistAmount);
        jsonAttServ.put("SEV", (Object)servAmount);
        jsonAttServ.put("EXC", (Object)expensesCollection);
        jsonAttServ.put("CUOTA", (Object)noquota);
        jsonAttServ.put("ISEXC", (Object)isExpensesCollection);
        jsonAttServ.put("SCSL_AddServ", jsonArrayAddServ);

        
        Invoice invoiceAttServ = null;
        BigDecimal paidAmountfinalquote = BigDecimal.ZERO;
        if (amountToPayment.compareTo(BigDecimal.ZERO) > 0 || isAdditionalAmount) {
            if (paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            	paidAmountfinalquote = paidAmount;
            }
            
            CustomSalesInvoice salesInvoice = null;
            BigDecimal amountToPaymentAttServ = BigDecimal.ZERO;
            
            if(amountToPayment.compareTo(BigDecimal.ZERO) > 0 || generateInvoiceAdditionalAmt ) {
            	salesInvoice = this.categories(conn, body, config, configLine, invoice, precision, paymentDate, config.getPaymentMethod(), jsonAttServ, quota, amountToPayment);
                invoiceAttServ = salesInvoice.getInvoice();
                scheduleDetails = (List<FIN_PaymentScheduleDetail>)dao.getInvoicePendingScheduledPaymentDetails(invoiceAttServ);
                
                paidAmount = (amountToPaymentAttServ = invoiceAttServ.getGrandTotalAmount());
                for (int i = 0; i < scheduleDetails.size(); ++i) {
                    final FIN_PaymentScheduleDetail item2 = scheduleDetails.get(i);
                    if (paidAmount.compareTo(item2.getAmount()) <= 0) {
                        paidAmounts.put(item2.getId(), paidAmount);
                        paidAmount = BigDecimal.ZERO;
                    }
                    else {
                        paidAmounts.put(item2.getId(), item2.getAmount());
                        paidAmount = paidAmount.subtract(item2.getAmount());
                    }
                    if (paidAmount.compareTo(BigDecimal.ZERO) == 0) {
                        i = scheduleDetails.size();
                    }
                    details.add(item2);
                }
            }
            
            
            final DocumentType documentType2 = config.getSwsslCDoctypePay();
            if (documentType2 == null) {
                throw new OBException("104,Document Type for Payment no found");
            }
            final String paymentNo3 = Utility.getDocumentNo(conn.getConnection(), conn, vars, "", "FIN_Payment", documentType2.getId(), documentType2.getId(), false, true);
            
            /*
             * Tk 12191
             * Autor: [Esteban Cuasapaz]
             * Funcionalidad: 
             * Este bloque de código ajusta el monto a procesar (amountToProcess) para evitar problemas
             * con decimales al procesar un cobro. Se elimina el exceso de decimales y se redondea a 2 decimales.
             */

            BigDecimal amountToProcess = amountToPayment.subtract(generateCredit.setScale(2, RoundingMode.HALF_UP)).add(amountToPaymentAttServ);
            BigDecimal amountToProcessRound = amountToProcess.setScale(2, RoundingMode.HALF_UP);

			if (isAdditionalAmount && amountToPayment.compareTo(BigDecimal.ZERO) <= 0) {
				amountToProcessRound = additionalAmount.setScale(2, RoundingMode.HALF_UP);
				amountToPay = amountToProcessRound;
            }
			
			FIN_Payment newPayment3 = null;
			if (!generateInvoiceAdditionalAmt && amountToPayment.compareTo(BigDecimal.ZERO) <= 0) {
				List<FIN_PaymentScheduleDetail> selectedPaymentDetails = new ArrayList<FIN_PaymentScheduleDetail>();
	            HashMap<String, BigDecimal> selectedPaymentDetailsAmounts = new HashMap<String, BigDecimal>();
	            newPayment3 = FIN_AddPayment.savePayment((FIN_Payment)null, (boolean)invoice.isSalesTransaction(), 
	            		documentType2, paymentNo3, customer, config.getSwsslPaymentmethodAnt(), quota.getAdvanceaccount(), 
	            		paidAmount.toString(), 
	            		paymentDate, org, (String)null, (List)selectedPaymentDetails, (HashMap)selectedPaymentDetailsAmounts, isWriteOff, false);
	            
    			GLItem glitem = quota.getGLItem();
    			FIN_AddPayment.saveGLItem(newPayment3, additionalAmount,
    			        OBDal.getInstance().get(GLItem.class, glitem.getId()), invoice.getBusinessPartner(), null, null, null,null, null, null, null, null);
    			

    			newPayment3.setAmount(newPayment3.getAmount().setScale(2, RoundingMode.HALF_UP).add(additionalAmount));
    			newPayment3.setFinancialTransactionAmount(newPayment3.getAmount().setScale(2, RoundingMode.HALF_UP).add(additionalAmount));
    		
				
			} else {
				newPayment3 = FIN_AddPayment.savePayment((FIN_Payment)null, (boolean)invoice.isSalesTransaction(), 
	            		documentType2, paymentNo3, customer, config.getSwsslPaymentmethod(), quota.getPaymentaccount(),
	            		amountToProcessRound.toString(),
	            		paymentDate, org, (String)null, (List)details, (HashMap)paidAmounts, isWriteOff, false);
			}
		
            
            newPayment3.setShpicNodocumeennt(invoice);
            newPayment3.setShpicNoquotescharg(ShpicNoquotescharg);
            newPayment3.setShpicPaymentchannelcharg(channelpayment);
            //newPayment3.setAccount(paymentAccount);
            newPayment3.setShpicProductcharg(invoice.getShpicProduct());
            newPayment3.setShpicByscsprcharg(invoice.getShpicByscspr());
            newPayment3.setSwsslImportChargedTotal(amountToPayOrigin);
            newPayment3.setSwsslCompany(company);
            if(isAdditionalAmount) {
            	newPayment3.setShpcfAdditionalvalue(additionalAmount);
            }

            if (paidAmountfinalquote.compareTo(BigDecimal.ZERO) > 0) {
	            final List<FIN_PaymentScheduleDetail> selectedPaymentDetails2 = new ArrayList<FIN_PaymentScheduleDetail>();
	            final HashMap<String, BigDecimal> selectedPaymentDetailsAmounts2 = new HashMap<String, BigDecimal>();
	            final DocumentType documentTypefinalquote = config.getSwsslDoctypeAdvance();
	            final String strAction2 = "PRD";
	            final String paymentNo2 = Utility.getDocumentNo(conn.getConnection(), conn, vars, "", "FIN_Payment", documentTypefinalquote.getId(), documentTypefinalquote.getId(), false, true);
	            final FIN_Payment newPayment2 = FIN_AddPayment.savePayment((FIN_Payment)null, (boolean)invoice.isSalesTransaction(), documentTypefinalquote, paymentNo2, customer, config.getSwsslPaymentmethodAnt(), quota.getAdvanceaccount(), paidAmountfinalquote.toString(), paymentDate, org, (String)null, (List)selectedPaymentDetails2, (HashMap)selectedPaymentDetailsAmounts2, isWriteOff, false);
	            newPayment2.setShpicNodocumeennt(invoice);
	            newPayment2.setShpicNoquotescharg(ShpicNoquotescharg);
                newPayment2.setShpicPaymentchannelcharg(channelpayment);
                //newPayment2.setAccount(paymentAccount);
	            newPayment2.setShpicProductcharg(invoice.getShpicProduct());
	            newPayment2.setShpicByscsprcharg(invoice.getShpicByscspr());
	            newPayment2.setSwsslPaymentOrigin(newPayment3);
	            newPayment2.setSwsslImportChargedTotal(amountToPayOrigin);
                newPayment2.setSwsslCompany(company);

	            final OBError message2 = FIN_AddPayment.processPayment(vars, conn, (strAction2.equals("PRP") || strAction2.equals("PPP")) ? "P" : "D", newPayment2, (String)null);
	            if ("Error".equals(message2.getType())) {
	                throw new OBException(message2.getMessage());
	            }
	            final BigDecimal shpicAdvancevaluelast = (invoice.getShpicAdvancevalue() == null) ? BigDecimal.ZERO : invoice.getShpicAdvancevalue();
	            invoice.setShpicAdvancevalue(shpicAdvancevaluelast.add(paidAmountfinalquote));
	            OBDal.getInstance().save((Object)invoice);
	            OBDal.getInstance().flush();
	            OBDal.getInstance().refresh((Object)invoice);
        	}
            if(newPaymentAdv != null) {
            	newPaymentAdv.setSwsslPaymentOrigin(newPayment3);  
            	indice = ShpicNoquotescharg.lastIndexOf('*'); // Encuentra la última aparición de '*'
                String ultimoNumero = ShpicNoquotescharg.substring(indice + 1); // Obtiene todo después del '*'
                Long totalquota =invoice.getShpicNodues();
                long UltimaCuotaLong = Long.parseLong(ultimoNumero);
                if (newPaymentAdv.getSwsslPaymentOrigin() != null) {
                    if(UltimaCuotaLong<totalquota) {
                    	if(UltimaCuotaLong==0) {
                    		newPaymentAdv.setShpicNoquotescharg("1");
                    	}else {
	                	UltimaCuotaLong+=1;
	                	String StringUltimaCuota=String.valueOf(UltimaCuotaLong);
	                	newPaymentAdv.setShpicNoquotescharg(StringUltimaCuota);
                    	}
                    }else {
	                	newPaymentAdv.setShpicNoquotescharg(ultimoNumero);
                    }
                }
                OBDal.getInstance().save((Object)newPaymentAdv);
                OBDal.getInstance().flush();
            }
            
            if(salesInvoice != null && salesInvoice.getLinesNTB().size() > 0) {
            	for(CustomSalesInvoice2 customLine : salesInvoice.getLinesNTB()) {
        			InvoiceLine line = customLine.getInvoiceLine();
        			GLItem glitem = customLine.getGlItem();
        			FIN_AddPayment.saveGLItem(newPayment3, line.getLineNetAmount(),
        			        OBDal.getInstance().get(GLItem.class, glitem.getId()), invoice.getBusinessPartner(), null, null, null,null, null, null, null, null);
        			

        			newPayment3.setAmount(newPayment3.getAmount().setScale(2, RoundingMode.HALF_UP).add(line.getLineNetAmount()));
        			newPayment3.setFinancialTransactionAmount(newPayment3.getAmount().setScale(2, RoundingMode.HALF_UP).add(line.getLineNetAmount()));
        		}
            }
       
            if(newPayment3.getDocumentType() != null && newPayment3.getDocumentType().isShpfrIscreditope()) {
            	DocumentType doctype = newPayment3.getDocumentType();
            	if(doctype.getDocumentCategory().equals("ARR")) { //AR Recibo
            		newPayment3.setShpseMailSendPayre(true);;
            	}
            }
            
            OBDal.getInstance().save((Object)newPayment3);
            OBDal.getInstance().flush();
            
            if (selectedCreditLines.length() > 0 && amountToPayment.compareTo(BigDecimal.ZERO) > 0) {
                this.logger.info((Object)selectedCreditLines.toString());
                this.addCredit(newPayment3, selectedCreditLines, BigDecimal.ZERO, paymentNo3, true, invoice);
            }
            
            for (final FIN_PaymentScheduleDetail psd2 : details) {
                final String psdId = psd2.getInvoicePaymentSchedule().getId();
                whereClause = new StringBuilder();
                whereClause.append(" WHERE ");
                whereClause.append(" TRIM('" + psdId + "') IN (" + "id" + ")");
                final OBQuery<FIN_PaymentSchedule> paymentSchedule = (OBQuery<FIN_PaymentSchedule>)OBDal.getInstance().createQuery((Class)FIN_PaymentSchedule.class, whereClause.toString());
                if (paymentSchedule.list().size() == 0) {
                    throw new OBException("103,Cuota Plan de pagos no existe");
                }
                final FIN_PaymentSchedule finPaymentSchedule = paymentSchedule.list().get(0);
                finPaymentSchedule.setShppsFeeStatus("PYT");
            }
            
            final String strDocAction = "P";
            OBError message2 = null;
            
            if (quota.isPaymenttransaction()||quota.isAdvancetransaction()) {
            	message2 = FIN_AddPayment.processPayment(vars, conn, strDocAction, newPayment3);
            }
            else {
                message2 = FIN_AddPayment.processPayment(vars, conn, strDocAction, newPayment3, "TRANSACTION");
            }
            
            if ("Error".equals(message2.getType())) {
                throw new OBException(message2.getMessage());
            }
            
            if(amountToPayment.compareTo(BigDecimal.ZERO) > 0) {
            	OBDal.getInstance().refresh((Object)newPayment3);
                Date shpicProxdues = null;
                BigDecimal shpicMostoverInstall = BigDecimal.ZERO;
                PreparedStatement sqlQuerySchedule = null;
                ResultSet result = null;
                final String sqlresult = "select duedate as datenextquote, coalesce((em_shpps_capital + em_shpps_attendance + em_shpps_service + em_shpps_expenses_collection + em_scsl_addservices), 0) as nextquote from fin_payment_schedule where c_invoice_id = ? and paidamt = 0 and duedate = (select min(duedate) from fin_payment_schedule where c_invoice_id = ? and paidamt = 0)";
                sqlQuerySchedule = new DalConnectionProvider(false).getPreparedStatement(sqlresult);
                sqlQuerySchedule.setString(1, invoice.getId());
                sqlQuerySchedule.setString(2, invoice.getId());
                sqlQuerySchedule.execute();
                result = sqlQuerySchedule.getResultSet();
                while (result.next()) {
                    shpicProxdues = result.getDate("datenextquote");
                    shpicMostoverInstall = result.getBigDecimal("nextquote");
                    invoice.setShpicProxdues(shpicProxdues);
                    invoice.setShpicMostoverInstall(shpicMostoverInstall);
                }
                Boolean iscompPayment = invoice.isPaymentComplete();            
                if(iscompPayment){
                	invoice.setShpicOperationState("05");
                }
                invoice.setShpicLastduespaid(noquota);
                invoice.setShpicPaymentUpdate(true);
                invoice.setShpicExpirationUpdate(true);
                if(invoice.getShpicNodues() != null && invoice.getShpicNodues() == noquota) {
                	invoice.setShpicOperationState("05");//05 cancelado //03 vencido //02 vigente
                }
                OBDal.getInstance().save((Object)invoice);
                OBDal.getInstance().flush();
                OBDal.getInstance().refresh((Object)invoice);
                
                
                boolean isExpCol = false;
                if (jsonAttServ.has("ISEXC")) {
                	isExpCol = new Boolean(jsonAttServ.get("ISEXC").toString());
    	            if(isExpCol) {
    		        	invoice.setSwsslIsrefinancing(Boolean.valueOf(true));
    		            final List<Object> parameters = new ArrayList<Object>();
    		            parameters.add(invoice.getId());
    		            parameters.add("COB");
    		            parameters.add(null);
    		            final String procedureName = "swssl_update_invoice";
    		            CallStoredProcedure.getInstance().call(procedureName, (List)parameters, (List)null, true, false);
    		            OBDal.getInstance().refresh((Object)invoice);        
    		        }
                }
            }
            
            if(amountToPayment.compareTo(BigDecimal.ZERO) <= 0 && isAdditionalAmount && newPaymentAdvAdditionalVal != null && newPayment3 != null) {
            	newPaymentAdvAdditionalVal.setSwsslPaymentOrigin(newPayment3);
            	OBDal.getInstance().save(newPaymentAdvAdditionalVal);
                OBDal.getInstance().flush();
            }
            
            body.put("paymentId", (Object)newPayment3.getId());
            body.put("paymentNo", (Object)newPayment3.getDocumentNo());
            
        }
        return jsonAttServ;
    }
    
    private CustomSalesInvoice categories(final ConnectionProvider conn, final JSONObject body, final shppws_config config, final List<scsl_Creditservices> configLine, final Invoice invoice, final int precision, final Date paymentDate, final FIN_PaymentMethod paymentMethod, final JSONObject jsonTotAttServ, final ShpcfCollectionQuota quota, final BigDecimal amountToPayment) throws Exception {
        final org.openbravo.base.secureApp.VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
        final boolean isWriteOff = false;
        final String paymenchannel = Swssl_Helper.getString(body, "paymenchannel", true);
        final CustomSalesInvoice invoiceAttServ = this.serviceSalesInvoice(conn, config, configLine, invoice, precision, paymentDate, paymentMethod, jsonTotAttServ, paymenchannel, quota);
        body.put("invoiceIdAttServ", (Object)invoiceAttServ.getInvoice().getId());
        body.put("invoiceNoAttServ", (Object)invoiceAttServ.getInvoice().getDocumentNo());
        return invoiceAttServ;
    }
    
    
    private CustomSalesInvoice serviceSalesInvoice(final ConnectionProvider conn, final shppws_config config, final List<scsl_Creditservices> configLine, final Invoice invoice, final int precision, final Date paymentDate, final FIN_PaymentMethod paymentMethod, final JSONObject jsonTotAttServ, final String paymenchannel, final ShpcfCollectionQuota quota) throws Exception {
        final Invoice newInvoice = (Invoice)OBProvider.getInstance().get((Class)Invoice.class);
        newInvoice.setSalesTransaction(Boolean.valueOf(true));
        newInvoice.setClient(invoice.getClient());
        newInvoice.setOrganization(invoice.getOrganization());
        newInvoice.setInvoiceDate(paymentDate);
        newInvoice.setAccountingDate(paymentDate);
        newInvoice.setDocumentStatus("DR");
        newInvoice.setDocumentAction("CO");
        newInvoice.setProcessed(Boolean.valueOf(false));
        newInvoice.setProcessNow(Boolean.valueOf(false));
        final org.openbravo.base.secureApp.VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
        final DocumentType documentType = config.getSwsslCDoctypeInv();
        if (documentType == null) {
            throw new OBException("104,Document Type for Invoice A/S not found for organization [" + invoice.getOrganization().getName() + "]");
        }
        final String invoiceNo = Utility.getDocumentNo(conn.getConnection(), conn, vars, "", "Invoice", documentType.getId(), documentType.getId(), false, true);
        if (invoiceNo == null || invoiceNo.length() == 0) {
            throw new OBException("104,Sequence Documentno Invoice no found for Invoice A/S Document Type [" + documentType.getName() + "]");
        }
        newInvoice.setDocumentType(documentType);
        newInvoice.setTransactionDocument(documentType);
        newInvoice.setDocumentNo(invoiceNo);
        newInvoice.setPaymentMethod(config.getSwsslPaymentmethod());
        newInvoice.setBusinessPartner(invoice.getBusinessPartner());
        newInvoice.setPartnerAddress(invoice.getPartnerAddress());
        newInvoice.setCurrency(invoice.getCurrency());
        newInvoice.setPaymentTerms(config.getSwsslCPaymenttermInv());
        newInvoice.setPriceList(invoice.getPriceList());
        newInvoice.setCostcenter(config.getSwsslCostCenterInv());
        newInvoice.setShpicNodocument(invoice);
        newInvoice.setShpicNoquoteess(jsonTotAttServ.get("CUOTA").toString());
        newInvoice.setShpicPaymentchannel(channelName);
        newInvoice.setSwsslInvoiceAS(true);
        OBDal.getInstance().save((Object)newInvoice);
        OBDal.getInstance().flush();
        Long lineNo = new Long(10L);
        List<CustomSalesInvoice2> linesNTB = new ArrayList<CustomSalesInvoice2>();
        for (final scsl_Creditservices prodCredits : configLine) {
            final Product product = prodCredits.getProduct();
            if (product == null) {
                throw new OBException("104,Service Credit Producto no found for [" + prodCredits.getCommercialName() + "]");
            }
            final String typeProduct = prodCredits.getSwsslTypeprod();
            if (typeProduct == null) {
                throw new OBException("104,Type Product no found for [" + prodCredits.getCommercialName() + "]");
            }
            BigDecimal amount = BigDecimal.ZERO;
            if(prodCredits.getSwsslTypeprod().equals("SCSL_AddServ") && jsonTotAttServ.has(prodCredits.getSwsslTypeprod())) {
            	JSONArray arrayAddServices = jsonTotAttServ.getJSONArray("SCSL_AddServ");
            	BigDecimal totalAmt = BigDecimal.ZERO;
                int count = 0;
            	for (int i = 0; i < arrayAddServices.length(); i++) {
            	    JSONObject obj = arrayAddServices.getJSONObject(i);
            	    String typeAddService = obj.getString("typeAddService");
            	    String idProdAddService = obj.getString("idProdAddService");
            	    String id = obj.getString("id");
            	    BigDecimal amt = new BigDecimal(obj.getString("amt"));
            	    if(id.equals(prodCredits.getId())) {
            	    	//amount = amount.add(amt);
            	    	amount = amount.add(amt.multiply(new BigDecimal(cuotasPagadas)));
            	    	break;
            	    }
            	}
            }else if (jsonTotAttServ.has(prodCredits.getSwsslTypeprod())) {
                amount = new BigDecimal(jsonTotAttServ.get(prodCredits.getSwsslTypeprod()).toString());
            } /*else if(prodCredits.getSwsslTypeprod().equals("NTB")) {
            	amount = amountNotBill( jsonTotAttServ, prodCredits);
            }*/
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            final OBCriteria<TaxRate> qTaxRate = (OBCriteria<TaxRate>)OBDal.getInstance().createCriteria((Class)TaxRate.class);
            qTaxRate.add((Criterion)Restrictions.eq("active", (Object)true));
            qTaxRate.add((Criterion)Restrictions.eq("taxCategory", (Object)product.getTaxCategory()));
            qTaxRate.setMaxResults(1);
            if (qTaxRate.list().size() == 0) {
                throw new OBException("Tax rate not found");
            }
            final TaxRate taxRate = qTaxRate.list().get(0);
            final BigDecimal rate = taxRate.getRate().divide(new BigDecimal(100), precision, RoundingMode.DOWN);
            BigDecimal baseAmount = amount;
            Currency objCurrency = OBDal.getInstance().get(Currency.class, "100");
            if(objCurrency == null) {
            	 throw new OBException("104, Currency not found.");
            }
            if (rate.compareTo(BigDecimal.ZERO) > 0) {
                baseAmount = amount.divide(rate.add(new BigDecimal("1")), objCurrency.getPricePrecision().intValue(), RoundingMode.DOWN);
                baseAmount = baseAmount.setScale(2, RoundingMode.HALF_UP);
            }
            final InvoiceLine line = (InvoiceLine)OBProvider.getInstance().get((Class)InvoiceLine.class);
            line.setInvoice(newInvoice);
            line.setClient(newInvoice.getClient());
            line.setOrganization(newInvoice.getOrganization());
            line.setLineNo(lineNo);
            line.setProduct(product);
            line.setSprliIdentifier(product.getSearchKey());
            line.setUOM(product.getUOM());
            line.setInvoicedQuantity(new BigDecimal("1"));
            line.setUnitPrice(baseAmount);
            line.setStandardPrice(baseAmount);
            line.setLineNetAmount(baseAmount);
            line.setListPrice(baseAmount);
            line.setTaxAmount(amount.subtract(baseAmount));
            line.setTax(taxRate);
            line.setSsbodDiscountRate(BigDecimal.ZERO);
            line.setSseedDiscount(BigDecimal.ZERO);
            line.setCostcenter(newInvoice.getCostcenter());
            line.setStDimension(newInvoice.getStDimension());
            prodCredits.getGLItem();
            if(!prodCredits.isItsbillable()) {
              OBDal.getInstance().save((Object)line);
              OBDal.getInstance().flush();
              lineNo += 10L;
            } else {
            	line.setLineNetAmount(amount);
            	linesNTB.add(new CustomSalesInvoice2(line, prodCredits.getGLItem()));
            }
        }
        if(isAdditionalAmount) {
            lineAdditionalService(newInvoice, additionalAmount, precision, lineNo, linesNTB, quota);
        }
        final List<Object> parameters = new ArrayList<Object>();
        parameters.add(null);
        parameters.add(newInvoice.getId());
        final String procedureName = "c_invoice_post";
        CallStoredProcedure.getInstance().call("c_invoice_post", (List)parameters, (List)null, true, false);
        OBDal.getInstance().refresh((Object)newInvoice);
        return new CustomSalesInvoice(newInvoice, linesNTB);
    }
    
    private void addCredit(final FIN_Payment payment, final JSONArray selectedCreditLines, final BigDecimal differenceAmount, final String strDifferenceAction, final Boolean st, final Invoice invoice) throws JSONException {
        BigDecimal remainingRefundAmt = differenceAmount;
        String strSelectedCreditLinesIds = null;
        if (selectedCreditLines.length() > 0) {
            strSelectedCreditLinesIds = this.getSelectedCreditLinesIds(selectedCreditLines);
            final List<FIN_Payment> selectedCreditPayment = (List<FIN_Payment>)FIN_Utility.getOBObjectList((Class)FIN_Payment.class, strSelectedCreditLinesIds);
            final HashMap<String, BigDecimal> selectedCreditPaymentAmounts = this.getSelectedCreditLinesAndAmount(selectedCreditLines, selectedCreditPayment);
            for (final FIN_Payment creditPayment : selectedCreditPayment) {
                final BusinessPartner businessPartner = creditPayment.getBusinessPartner();
                if (businessPartner == null) {
                    throw new OBException(OBMessageUtils.messageBD("APRM_CreditWithoutBPartner"));
                }
                String currency = null;
                if (businessPartner.getCurrency() == null) {
                    currency = creditPayment.getCurrency().getId();
                    businessPartner.setCurrency(creditPayment.getCurrency());
                }
                else {
                    currency = businessPartner.getCurrency().getId();
                }
                if (!creditPayment.getCurrency().getId().equals(currency)) {
                    throw new OBException(String.format(OBMessageUtils.messageBD("APRM_CreditCurrency"), businessPartner.getCurrency().getISOCode()));
                }
                BigDecimal usedCreditAmt = selectedCreditPaymentAmounts.get(creditPayment.getId());
                if (remainingRefundAmt.compareTo(usedCreditAmt) > 0) {
                    remainingRefundAmt = remainingRefundAmt.subtract(usedCreditAmt);
                    usedCreditAmt = BigDecimal.ZERO;
                }
                else {
                    usedCreditAmt = usedCreditAmt.subtract(remainingRefundAmt);
                    remainingRefundAmt = BigDecimal.ZERO;
                }
                creditPayment.setUsedCredit(usedCreditAmt.add(creditPayment.getUsedCredit()));
                if (usedCreditAmt.compareTo(BigDecimal.ZERO) > 0) {
                    final StringBuffer description = new StringBuffer();
                    if (creditPayment.getDescription() != null && !creditPayment.getDescription().equals("")) {
                        description.append(creditPayment.getDescription()).append("\n");
                    }
                    description.append(String.format(OBMessageUtils.messageBD("APRM_CreditUsedPayment"), payment.getDocumentNo()));
                    final String truncateDescription = (description.length() > 255) ? description.substring(0, 251).concat("...").toString() : description.toString();
                    creditPayment.setDescription(truncateDescription);
                    creditPayment.setSwsslCreditOpe(payment);
                    FIN_PaymentProcess.linkCreditPayment(payment, usedCreditAmt, creditPayment);
                    final BigDecimal shpicAdvancevalue1 = (invoice.getShpicAdvancevalue() == null) ? BigDecimal.ZERO : invoice.getShpicAdvancevalue();
                    invoice.setShpicAdvancevalue(shpicAdvancevalue1.subtract(usedCreditAmt));
                }
                OBDal.getInstance().save((Object)creditPayment);
            }
        }
    }
    
    private String getSelectedCreditLinesIds(final JSONArray allselection) throws JSONException {
        final StringBuilder sb = new StringBuilder();
        sb.append("(");
        for (int i = 0; i < allselection.length(); ++i) {
            final JSONObject selectedRow = allselection.getJSONObject(i);
            sb.append(String.valueOf(selectedRow.getString("id")) + ",");
        }
        sb.replace(sb.lastIndexOf(","), sb.lastIndexOf(",") + 1, ")");
        return sb.toString();
    }
    
    private HashMap<String, BigDecimal> getSelectedCreditLinesAndAmount(final JSONArray allselection, final List<FIN_Payment> _selectedCreditPayments) throws JSONException {
        final HashMap<String, BigDecimal> selectedCreditLinesAmounts = new HashMap<String, BigDecimal>();
        for (final FIN_Payment creditPayment : _selectedCreditPayments) {
            for (int i = 0; i < allselection.length(); ++i) {
                final JSONObject selectedRow = allselection.getJSONObject(i);
                if (selectedRow.getString("id").equals(creditPayment.getId())) {
                    selectedCreditLinesAmounts.put(creditPayment.getId(), new BigDecimal(selectedRow.getString("paymentAmount")));
                }
            }
        }
        return selectedCreditLinesAmounts;
    }
    
    private List<FIN_Payment> getAdvPayments(final String partnerId, final String invoiceid) {
        final StringBuffer hqlString = new StringBuffer();
        hqlString.append(" as p");
        hqlString.append(" WHERE generatedCredit<>0 ");
        hqlString.append(" AND usedCredit<>generatedCredit ");
        hqlString.append(" AND businessPartner.id=:partnerId ");
        hqlString.append(" AND shpicNodocumeennt.id=:invoiceid ");
        hqlString.append(" ORDER BY paymentDate,documentNo ");
        final OBQuery<FIN_Payment> advPayments = (OBQuery<FIN_Payment>)OBDal.getInstance().createQuery((Class)FIN_Payment.class, hqlString.toString());
        advPayments.setNamedParameter("partnerId", (Object)partnerId);
        advPayments.setNamedParameter("invoiceid", (Object)invoiceid);
        return (List<FIN_Payment>)advPayments.list();
    }
    
    private BigDecimal amountNotBill( JSONObject jsonTotAttServ, scsl_Creditservices prodCredits) {
    	BigDecimal amount = BigDecimal.ZERO;
    	try {
        	String service = prodCredits.getProduct().getName();
        	if(service.equals("ASISTENCIA")) {
        		service = "ATT";
        	}else if(service.equals("SERVICIO")) {
        		service = "SEV";
        	}else if(service.equals("GASTOS DE COBRANZA")) {
        		service = "EXC";
        	}
        	
        	if (jsonTotAttServ.has(service)) {
                amount = new BigDecimal(jsonTotAttServ.get(service).toString());
            }
    		
    	}catch(Exception e) {
    		e.printStackTrace();
    	}
    	
    	return amount;
    }
    
    private void lineAdditionalService(Invoice newInvoice, BigDecimal amount, int precision, Long lineNo, List<CustomSalesInvoice2> linesNTB, ShpcfCollectionQuota quota) {
    	final OBCriteria<TaxRate> qTaxRate = (OBCriteria<TaxRate>)OBDal.getInstance().createCriteria((Class)TaxRate.class);
        qTaxRate.add((Criterion)Restrictions.eq("active", (Object)true));
        qTaxRate.add((Criterion)Restrictions.eq("taxCategory", (Object)quota.getProduct().getTaxCategory()));
        qTaxRate.setMaxResults(1);
        if (qTaxRate.list().size() == 0) {
            throw new OBException("Tax rate not found");
        }
        
        final TaxRate taxRate = qTaxRate.list().get(0);
        final BigDecimal rate = taxRate.getRate().divide(new BigDecimal(100), precision, RoundingMode.DOWN);
        BigDecimal baseAmount = amount;
        Currency objCurrency = OBDal.getInstance().get(Currency.class, "100");
        if(objCurrency == null) {
        	 throw new OBException("104, Currency not found.");
        }
        if (rate.compareTo(BigDecimal.ZERO) > 0) {
            baseAmount = amount.divide(rate.add(new BigDecimal("1")), objCurrency.getPricePrecision().intValue(), RoundingMode.DOWN);
            baseAmount = baseAmount.setScale(2, RoundingMode.HALF_UP);
        }
        final InvoiceLine line = (InvoiceLine)OBProvider.getInstance().get((Class)InvoiceLine.class);
        line.setInvoice(newInvoice);
        line.setClient(newInvoice.getClient());
        line.setOrganization(newInvoice.getOrganization());
        line.setLineNo(lineNo);
        line.setProduct(quota.getProduct());
        line.setSprliIdentifier(quota.getProduct().getSearchKey());
        line.setUOM(quota.getProduct().getUOM());
        line.setInvoicedQuantity(new BigDecimal("1"));
        line.setUnitPrice(baseAmount);
        line.setStandardPrice(baseAmount);
        line.setLineNetAmount(baseAmount);
        line.setListPrice(baseAmount);
        line.setTaxAmount(amount.subtract(baseAmount));
        line.setTax(taxRate);
        line.setSsbodDiscountRate(BigDecimal.ZERO);
        line.setSseedDiscount(BigDecimal.ZERO);
        line.setCostcenter(newInvoice.getCostcenter());
        line.setStDimension(newInvoice.getStDimension());
        if(quota.isSueinvoice()) {
          OBDal.getInstance().save((Object)line);
          OBDal.getInstance().flush();
          lineNo += 10L;
        } else {
        	line.setLineNetAmount(amount);
        	linesNTB.add(new CustomSalesInvoice2(line, quota.getGLItem()));
        }
    }
}
