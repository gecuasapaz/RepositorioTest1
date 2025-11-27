package ec.com.sidesoft.closecash.report.print.ad_Reports;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
//import org.openbravo.base.model.Table;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.ui.Window;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.utils.Replace;
import org.openbravo.base.session.OBPropertiesProvider;

import ec.com.sidesoft.custom.reports.SescrTemplateReport;
import ec.com.sidesoft.custom.closecash.ScccCashClousure;
import org.openbravo.model.financialmgmt.payment.*;
import org.openbravo.model.common.invoice.Invoice;
import ec.com.sidesoft.closecash.sales.order.SsccsoTypeOfDocument;
import org.openbravo.model.common.enterprise.DocumentType;

@SuppressWarnings("serial")
public class CashClose extends HttpSecureAppServlet {

  private String strSessionValue = "";
  private String StrWindowdID = "";
  private String StrTableID = "";
  private String strADUSerID = "";

  private static Logger log4j1 = Logger.getLogger(CashClose.class);

  public void init(ServletConfig config) {
    super.init(config);
    boolHist = false;  
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {

    strSessionValue = "67C3F5060FE3451681828B742B3715A2|Sccc_Cash_Clousure_ID";
    StrWindowdID = "67C3F5060FE3451681828B742B3715A2";
    StrTableID = "B9AA966B759748ACBAB9E5D9552A6659";

    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {
      String strDocumentId = vars.getSessionValue(strSessionValue);

      // normalize the string of id to a comma separated list
      strDocumentId = strDocumentId.replaceAll("\\(|\\)|'", "");
      if (strDocumentId.length() == 0)
        throw new ServletException(Utility.messageBD(this, "NoDocument", vars.getLanguage()));

      if (log4j1.isDebugEnabled())
        log4j1.debug("strDocumentId: " + strDocumentId);
      printPagePDF(response, vars, strDocumentId);
    } else {
      pageError(response);

    }
  }

  private void printPagePDF(HttpServletResponse response, VariablesSecureApp vars,
      String strDocumentId) throws IOException, ServletException {
    ConnectionProvider conn = new DalConnectionProvider(false);
    String language = OBContext.getOBContext().getLanguage().getLanguage();
    Integer records = 0;

    String strAD_UserID = vars.getUser().toString();

    List<DocumentType> doctype = new ArrayList<DocumentType>();
    List<FIN_PaymentDetail> listdetaill = new ArrayList<FIN_PaymentDetail>();

    // Type Formulary
    Window ADWindow = OBDal.getInstance().get(Window.class, StrWindowdID);
    Table ADTable = OBDal.getInstance().get(Table.class, StrTableID);

    OBCriteria<SescrTemplateReport> PrintWithh = OBDal.getInstance()
        .createCriteria(SescrTemplateReport.class);
    PrintWithh.add(Restrictions.eq(SescrTemplateReport.PROPERTY_WINDOW, ADWindow));
    PrintWithh.add(Restrictions.eq(SescrTemplateReport.PROPERTY_TABLE, ADTable));
    // PrintWithh.add(Restrictions.eq(SescrTemplateReport.PROPERTY_ORGANIZATION, ADOrg)); optional

    List<SescrTemplateReport> LstTemplate = PrintWithh.list();
    int ICountTemplate; 
    BigDecimal sumtotal = new BigDecimal("0");

    ICountTemplate = LstTemplate.size();

    if (ICountTemplate == 0) {

      throw new ServletException(Utility.messageBD(conn, "@Template no Found..@", language));

    }
    ScccCashClousure ScccCashClousure = OBDal.getInstance().get(ScccCashClousure.class,
        strDocumentId);
    DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    String strDate = dateFormat.format(ScccCashClousure.getClosingdate());
    CloseCashValdtData data[] = CloseCashValdtData.select(conn, strDate, strDocumentId, strDate,
        strDocumentId, strDate, strDocumentId);

    if (data.length > 0) {
      System.out.println(data.length );
      for (CloseCashValdtData value : data) {
        sumtotal.add(new BigDecimal(value.sum));
      }
    }

    // Validacion transacciones no registradas
    if (data.length > 0 && sumtotal.compareTo(BigDecimal.ZERO) != 0
        && ScccCashClousure.getDocumentStatus().equals("DR")) {
      throw new ServletException(
          Utility.messageBD(conn, "Se requiere registrar el Cierre de Caja", language));
    }

    if (LstTemplate.get(0).getWindow().getId().equals(StrWindowdID)) {

      String StrRutaReport = LstTemplate.get(0).getTemplateDir();

      String strReportName = StrRutaReport + "/" + LstTemplate.get(0).getNameReport();

      final String strAttach = globalParameters.strFTPDirectory + "/284-" + classInfo.id;

      final String strLanguage = vars.getLanguage();

      final String strBaseDesign = getBaseDesignPath(strLanguage);

      strReportName = Replace.replace(Replace.replace(strReportName, "@basedesign@", strBaseDesign),
          "@attach@", strAttach);
      String sourcepath = OBPropertiesProvider.getInstance().getOpenbravoProperties()
          .getProperty("attach.path");

      if (log4j1.isDebugEnabled())
        log4j1.debug("Output: Multiphase Project - pdf");

      // VALIDACION PARA SQL

      HashMap<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("DOCUMENT_ID", strDocumentId);
      parameters.put("SETUP_ID", ScccCashClousure.getScccSetup().getId());
      String StrBaseWeb = getBaseDesignPath(strLanguage);
      parameters.put("BASE_WEB", StrBaseWeb);
      parameters.put("AD_USER_ID", strAD_UserID);
      parameters.put("SOURCE_PATH", sourcepath);
      String StrNameReport = "Rpt_" + LstTemplate.get(0).getTitle().replace(" ", "_");
      records = 0;
      // renderJR(vars, response, strReportName, "pdf", parameters, null, null);
      renderJR(vars, response, strReportName, StrNameReport, "pdf", parameters, null, null, false);

    }

  }

  public String getServletInfo() {
    return "Servlet that processes the print action";
  } // End of getServletInfo() method
}
