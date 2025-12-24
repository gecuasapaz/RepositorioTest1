package ec.com.sidesoft.fast.quotation.ad_action;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.data.FieldProvider;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.DateTimeData;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.enterprise.OrgWarehouse;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.PaymentTerm;
import org.openbravo.model.financialmgmt.tax.TaxCategory;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.model.pricing.priceadjustment.PriceAdjustment;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.model.pricing.pricelist.PriceListVersion;
import org.openbravo.model.pricing.pricelist.ProductPrice;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.xmlEngine.XmlDocument;

import ec.com.sidesoft.credit.factory.SscfCreditRange;
import ec.com.sidesoft.credit.factory.SscfProfileCreditRisk;
import ec.com.sidesoft.fast.quotation.ECSFQSpecialcondition;
import ec.com.sidesoft.fast.quotation.ECSFQ_Quotation;
import ec.com.sidesoft.fast.quotation.EcsfqPromoLine;
import ec.com.sidesoft.fast.quotation.Promotion;
import ec.com.sidesoft.fast.quotation.QuotationLine;
import ec.com.sidesoft.fast.quotation.QuotationLineTax;
import ec.com.sidesoft.fast.quotation.QuotationOpportunity;
import ec.com.sidesoft.fast.quotation.dao.MethodsDao;
import ec.com.sidesoft.fast.quotation.dao.PaymentType;
import ec.com.sidesoft.vehicle.registration.svhreRegistration;
import it.openia.crm.Opcrmopportunities;

public class Quotation extends HttpSecureAppServlet {

  private static final long serialVersionUID = 1L;
  private final int scale = 10;

  OBError msg = new OBError();
  boolean status = true;
  String state = "";

  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    User user = OBContext.getOBContext().getUser();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    String promociones = "N";
    if (vars.commandIn("DEFAULT")) {
      state = "C";
      status = true;
      String paymentTermId = "";
      String paymentTypeId = "";
      String opportunitiesId = vars.getStringParameter("inpopcrmOpportunitiesId");
      String strOrgId = vars.getRequestGlobalVariable("inpadOrgId", "Reconciliation|Org");
      String strWindowId = vars.getRequestGlobalVariable("inpwindowId", "Reconciliation|windowId");
      String strTabId = vars.getRequestGlobalVariable("inpTabId", "Reconciliation|tabId");
      boolean verifylocation = verifylocationpartner(opportunitiesId);
      if(!verifylocation) {
    	  advisePopUp(request, response, "ERROR", "Error",
    	          "No se ha encontrado direccion del tercero / consulte en la ventana de Terceros solapa Direcciones");
      }else {
          removeAll(response, opportunitiesId);
          printPage(response, vars, "51DB006F2FA54BDDAAFFCF263341295E",
              "94EAA455D2644E04AB25D93BE5157B6D", "2845D761A8394468BD3BA4710AA888D4", opportunitiesId,
              null, null, status, state, paymentTermId, paymentTypeId, promociones);
      }
    } else if (vars.commandIn("ADD")) {
      String paymentTermId = vars.getStringParameter("inpCondicionId");
      String paymentTypeId = vars.getStringParameter("inpFormaPId");
      state = "A";
      status = true;
      String opportunitiesId = vars.getStringParameter("inpOpportunityId", "");
      String strProductId = vars.getStringParameter("inpmProductId");
      if (strProductId.length() < 1) {
        updateAll(opportunitiesId);
        printPage(response, vars, "51DB006F2FA54BDDAAFFCF263341295E",
            "94EAA455D2644E04AB25D93BE5157B6D", "2845D761A8394468BD3BA4710AA888D4", opportunitiesId,
            null, null, status, state, paymentTermId, paymentTypeId, promociones);
      } else {
        promociones = "Y";
        String strMetodoId = vars.getStringParameter("inpMetodoId");
        String strCodicionId = vars.getStringParameter("inpCondicionId");
        String strEntrada = vars.getStringParameter("inpEntrada");
        String strListaPrecios = vars.getStringParameter("inpListaPrecios");

        status = addProduct(strProductId, strCodicionId, strMetodoId, strEntrada, "0",
            strListaPrecios, opportunitiesId, paymentTermId);
        updateAll(opportunitiesId);
        printPage(response, vars, "51DB006F2FA54BDDAAFFCF263341295E",
            "94EAA455D2644E04AB25D93BE5157B6D", "2845D761A8394468BD3BA4710AA888D4", opportunitiesId,
            null, null, status, state, paymentTermId, paymentTypeId, promociones);
      }
    } else if (vars.commandIn("SELECTED")) {
      String strSelectedTransId = vars.getStringParameter("inpCurrentTransIdSelected", "");
      String opportunitiesId = vars.getStringParameter("inpOpportunityId", "");
      String strSelectedCheck = vars.getStringParameter("inpIsCurrentTransSelected", "");
      QuotationOpportunity opportunity = OBDal.getInstance().get(QuotationOpportunity.class,
          strSelectedTransId);

      try {
        OBContext.setAdminMode(true);

        if (strSelectedCheck.equals("true")) {
          opportunity.setAlertStatus(true);
        } else {
          opportunity.setAlertStatus(false);
        }
      } finally {
        OBContext.restorePreviousMode();
      }

    } else if (vars.commandIn("PROCESS")) {
      String opportunitiesId = vars.getStringParameter("inpOpportunityId", "");
      String paymentTermId = vars.getStringParameter("inpCondicionId");
      String paymentTypeId = vars.getStringParameter("inpFormaPId");
      try {
        addQuotation(opportunitiesId, paymentTypeId);
      } catch (Exception e) {
        try {
          OBDal.getInstance().getConnection().rollback();
        } catch (SQLException e1) {
          e1.printStackTrace();
        }
        e.printStackTrace();
      }
      msg = new OBError();
      paymentTypeId = "";
      paymentTermId = "";
      msg.setType("Success");
      msg.setTitle(Utility.messageBD(this, "Success", vars.getLanguage()));
      msg.setMessage(
          Utility.parseTranslation(this, vars, vars.getLanguage(), "@ecsfq_add_quotation@"));
      vars.setMessage("2B2322A0933B42F0800745C983D64A5B", msg);
      msg = null;
      removeAll(response, opportunitiesId);
      printPageClosePopUpAndRefreshParent(response, vars);
    } else if (vars.commandIn("CLOSE")) {
      String opportunitiesId = vars.getStringParameter("inpOpportunityId", "");
      try {
        OBContext.setAdminMode(true);
        removeAll(response, opportunitiesId);
      } finally {
        printPageClosePopUpAndRefreshParent(response, vars);
        OBContext.restorePreviousMode();
      }
    } else if (vars.commandIn("REMOVE")) {
      state = "R";
      status = true;
      String opportunitiesId = vars.getStringParameter("inpOpportunityId", "");
      String paymentTermId = vars.getStringParameter("inpCondicionId");
      String paymentTypeId = vars.getStringParameter("inpFormaPId");

      try {
        OBContext.setAdminMode(true);
        removeProduct(response, opportunitiesId);
      } finally {
        printPage(response, vars, "51DB006F2FA54BDDAAFFCF263341295E",
            "94EAA455D2644E04AB25D93BE5157B6D", "2845D761A8394468BD3BA4710AA888D4", opportunitiesId,
            null, null, status, state, paymentTermId, paymentTypeId, promociones);
        OBContext.restorePreviousMode();
      }

    } else if (vars.commandIn("GRID")) {
      String opportunitiesId = vars.getStringParameter("inpOpportunityId", "");
      printGrid(response, opportunitiesId);
    } else if (vars.commandIn("GRIDTWO")) {
      String opportunitiesId = vars.getStringParameter("inpOpportunityId", "");
      printSecondGrid(response, opportunitiesId);
    } else if (vars.commandIn("ReloadCombo")) {
      OBContext.setAdminMode(true);
      String paymentTermId = vars.getStringParameter("inpCondicionId");
      String paymentType = vars.getStringParameter("inpFormaPId", "");
      reloadPaymentTypeCombo(response, paymentType, paymentTermId);
      OBContext.restorePreviousMode();
    } else if (vars.commandIn("CHANGE")) {
      OBContext.setAdminMode(true);
      String precioEst = "";
      state = "CH";
      status = true;
      String opportunitiesId = vars.getStringParameter("inpOpportunityId", "");
      String paymentTermId = vars.getStringParameter("inpCondicionId");
      String paymentTypeId = vars.getStringParameter("inpFormaPId");
      PaymentTerm condicionPago = OBDal.getInstance().get(PaymentTerm.class, paymentTermId);
      Opcrmopportunities opportunity = OBDal.getInstance().get(Opcrmopportunities.class,
          opportunitiesId);
      OBCriteria<QuotationOpportunity> proformaOBList = OBDal.getInstance()
          .createCriteria(QuotationOpportunity.class);
      proformaOBList
          .add(Restrictions.eq(QuotationOpportunity.PROPERTY_OPCRMOPPORTUNITIES, opportunity));
      OBContext.restorePreviousMode();
      try {
        OBContext.setAdminMode(true);
        for (int i = 0; i < proformaOBList.count(); i++) {
          if (!proformaOBList.list().get(i).getPaymentTerms().equals(condicionPago)) {
            Opcrmopportunities opportunityP = OBDal.getInstance().get(Opcrmopportunities.class,
                opportunitiesId);
            OBCriteria<QuotationOpportunity> qlt = OBDal.getInstance()
                .createCriteria(QuotationOpportunity.class);
            qlt.add(
                Restrictions.eq(QuotationOpportunity.PROPERTY_OPCRMOPPORTUNITIES, opportunityP));
            qlt.add(Restrictions.eq(QuotationOpportunity.PROPERTY_ISPROCESS, false));

            OBCriteria<Promotion> promotionList = OBDal.getInstance()
                .createCriteria(Promotion.class);
            promotionList.add(Restrictions.eq(Promotion.PROPERTY_OPCRMOPPORTUNITIES, opportunityP));
            if (qlt.count() > 0) {
              promotionList
                  .add(Restrictions.eq(Promotion.PROPERTY_ECSFQQUOTATION, qlt.list().get(0)));
            }
            for (Promotion promo : promotionList.list()) {
              OBDal.getInstance().remove(promo);
            }
          }
          precioEst = newPriceEstandar(proformaOBList.list().get(i), opportunitiesId,
              condicionPago);
          proformaOBList.list().get(i).setPaymentTerms(condicionPago);
          proformaOBList.list().get(i).setPrecioe(precioEst);
          proformaOBList.list().get(i).setAlertStatus(false);
          BigDecimal entry = getEntrada(proformaOBList.list().get(i).getProduct(), condicionPago,
              new BigDecimal(proformaOBList.list().get(i).getEntrada()),
              new BigDecimal(proformaOBList.list().get(i).getPrecioe()),
              proformaOBList.list().get(i), opportunity);
          BigDecimal registration = new BigDecimal(
              proformaOBList.list().get(i).getSvhreRegistrationAmt());
          proformaOBList.list().get(i).setEntrada(entry.toString());
          proformaOBList.list().get(i).setSvhreEntregAmt(entry.add(registration).toString());

          proformaOBList.list().get(i)
              .setCuota(getCreditFee(proformaOBList.list().get(i).getProduct(), condicionPago,
                  proformaOBList.list().get(i), opportunity).toString());
          proformaOBList.list().get(i).setImpuesto(getTotalTax(proformaOBList.list().get(i),
              condicionPago, proformaOBList.list().get(i).getProduct(), opportunity).toString());
          proformaOBList.list().get(i).setTotal(
              getTotalCreditAmount(proformaOBList.list().get(i), condicionPago).toString());
          SscfProfileCreditRisk profileCreditRisk = getProfileCreditRisConfiguration(
              proformaOBList.list().get(i).getProduct(), condicionPago,
              proformaOBList.list().get(i), opportunity);
          /*
           * if (profileCreditRisk == null & condicionPago.getOffsetMonthDue() > 0) { PaymentTerm
           * temp = condicionPago; temp.setName("Error");
           * proformaOBList.list().get(i).setPaymentTerms(temp); }
           */
        }
        OBDal.getInstance().flush();
      } finally {
        OBContext.restorePreviousMode();
      }
      printPage(response, vars, "51DB006F2FA54BDDAAFFCF263341295E",
          "94EAA455D2644E04AB25D93BE5157B6D", "2845D761A8394468BD3BA4710AA888D4", opportunitiesId,
          null, null, status, state, paymentTermId, paymentTypeId, promociones);
    } else if (vars.commandIn("ChangeEntry")) {
      String opportunitiesId = vars.getStringParameter("inpOpportunityId", "");
      OBContext.setAdminMode(true);
      String inpNewEntry = vars.getStringParameter("inpNewEntry");
      inpNewEntry = inpNewEntry.replaceAll("[^0-9.]", "");
      String inpFilaNum = vars.getStringParameter("inpFilaNum");
      QuotationOpportunity proformaOBList = OBDal.getInstance().get(QuotationOpportunity.class,
          inpFilaNum);
      Opcrmopportunities opportunity = OBDal.getInstance().get(Opcrmopportunities.class,
          opportunitiesId);
      proformaOBList.setEntrada(getEntrada(proformaOBList.getProduct(),
          proformaOBList.getPaymentTerms(), new BigDecimal(inpNewEntry),
          new BigDecimal(proformaOBList.getPrecioe()), proformaOBList, opportunity).toString());
      proformaOBList.setCuota(getCreditFee(proformaOBList.getProduct(),
          proformaOBList.getPaymentTerms(), proformaOBList, opportunity).toString());
      proformaOBList.setImpuesto(getTotalTax(proformaOBList, proformaOBList.getPaymentTerms(),
          proformaOBList.getProduct(), opportunity).toString());
      proformaOBList.setTotal(
          getTotalCreditAmount(proformaOBList, proformaOBList.getPaymentTerms()).toString());

      // proformaOBList.setSvhreRegistrationAmt(proformaOBList.getSvhreRegistrationAmt().toString());
      proformaOBList.setSvhreEntregAmt(getEntrada(proformaOBList.getProduct(),
          proformaOBList.getPaymentTerms(), new BigDecimal(inpNewEntry),
          new BigDecimal(proformaOBList.getPrecioe()), proformaOBList, opportunity)
              .add(new BigDecimal(proformaOBList.getSvhreRegistrationAmt())).toString());

      SscfProfileCreditRisk profileCreditRisk = getProfileCreditRisConfiguration(
          proformaOBList.getProduct(), proformaOBList.getPaymentTerms(), proformaOBList,
          opportunity);
      if (profileCreditRisk == null & proformaOBList.getPaymentTerms().getOffsetMonthDue() > 0) {
        PaymentTerm temp = proformaOBList.getPaymentTerms();
        temp.setName("Error");
        proformaOBList.setPaymentTerms(temp);
      }
      OBContext.restorePreviousMode();
    } else if (vars.commandIn("RELOAD")) {

      String paymentTermId = vars.getStringParameter("inpCondicionId");
      String paymentTypeId = vars.getStringParameter("inpFormaPId");
      String opportunitiesId = vars.getStringParameter("inpOpportunityId", "");
      printPage(response, vars, "51DB006F2FA54BDDAAFFCF263341295E",
          "94EAA455D2644E04AB25D93BE5157B6D", "2845D761A8394468BD3BA4710AA888D4", opportunitiesId,
          null, null, status, state, paymentTermId, paymentTypeId, promociones);
    }
  }

  private void removeProduct(HttpServletResponse response, String opportunitiesId) {

    Opcrmopportunities opportunity = OBDal.getInstance().get(Opcrmopportunities.class,
        opportunitiesId);
    try {
      OBContext.setAdminMode(true);

      OBCriteria<QuotationOpportunity> quotationList = OBDal.getInstance()
          .createCriteria(QuotationOpportunity.class);
      quotationList.add(Restrictions.eq(QuotationOpportunity.PROPERTY_ALERTSTATUS, true));
      quotationList
          .add(Restrictions.eq(QuotationOpportunity.PROPERTY_OPCRMOPPORTUNITIES, opportunity));

      if (quotationList.count() > 0) {

        for (QuotationOpportunity quotation : quotationList.list()) {

          try {
            OBCriteria<Promotion> promotionList = OBDal.getInstance()
                .createCriteria(Promotion.class);
            promotionList.add(Restrictions.eq(Promotion.PROPERTY_OPCRMOPPORTUNITIES, opportunity));
            promotionList.add(Restrictions.eq(Promotion.PROPERTY_ECSFQQUOTATION, quotation));

            if (promotionList.count() > 0) {
              for (Promotion promotion : promotionList.list()) {
                OBDal.getInstance().remove(promotion);
                OBDal.getInstance().flush();
              }
            }
          } catch (Exception e) {
            throw new OBException(e);
          }

          OBDal.getInstance().remove(quotation);
          OBDal.getInstance().flush();
        }
      }
    } catch (Exception e) {
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  private void removePromotions(String opportunitiesId) {

    Opcrmopportunities opportunity = OBDal.getInstance().get(Opcrmopportunities.class,
        opportunitiesId);

    try {
      OBContext.setAdminMode(true);

      OBCriteria<Promotion> promotionList = OBDal.getInstance().createCriteria(Promotion.class);
      promotionList.add(Restrictions.eq(Promotion.PROPERTY_OPCRMOPPORTUNITIES, opportunity));

      if (promotionList.count() > 0) {
        for (Promotion promotion : promotionList.list()) {
          OBDal.getInstance().remove(promotion);
          OBDal.getInstance().flush();
        }
      }
    } catch (Exception e) {
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  private void removeAll(HttpServletResponse response, String opportunitiesId) {

    Opcrmopportunities opportunity = OBDal.getInstance().get(Opcrmopportunities.class,
        opportunitiesId);

    try {
      OBContext.setAdminMode(true);
      OBCriteria<Promotion> promotionList = OBDal.getInstance().createCriteria(Promotion.class);
      promotionList.add(Restrictions.eq(Promotion.PROPERTY_OPCRMOPPORTUNITIES, opportunity));

      if (promotionList.count() > 0) {
        for (Promotion promotion : promotionList.list()) {
          OBDal.getInstance().remove(promotion);
          OBDal.getInstance().flush();
        }
      }
    } catch (Exception e) {
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
    

    try {
      OBContext.setAdminMode(true);
      OBCriteria<QuotationOpportunity> quotationList = OBDal.getInstance()
          .createCriteria(QuotationOpportunity.class);
      quotationList
          .add(Restrictions.eq(QuotationOpportunity.PROPERTY_OPCRMOPPORTUNITIES, opportunity));
      if (quotationList.count() > 0) {
        for (QuotationOpportunity quotation : quotationList.list()) {
          OBDal.getInstance().remove(quotation);
          OBDal.getInstance().flush();
        }
      }
    } catch (Exception e) {
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  private void updateAll(String opportunitiesId) {
    Opcrmopportunities opportunity = OBDal.getInstance().get(Opcrmopportunities.class,
        opportunitiesId);
    try {
      OBContext.setAdminMode(true);
      OBCriteria<QuotationOpportunity> quotationList = OBDal.getInstance()
          .createCriteria(QuotationOpportunity.class);
      quotationList.add(Restrictions.eq(QuotationOpportunity.PROPERTY_ALERTSTATUS, true));
      quotationList
          .add(Restrictions.eq(QuotationOpportunity.PROPERTY_OPCRMOPPORTUNITIES, opportunity));
      if (quotationList.count() > 0) {
        for (QuotationOpportunity quotation : quotationList.list()) {
          quotation.setAlertStatus(false);
        }
      }
    } catch (Exception e) {
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  private boolean addProduct(String productId, String condicionId, String metodoId, String entrada,
      String descuento, String priceList, String opportunitiesId, String paymentTermId) {

    try {
      OBContext.setAdminMode(true);
      String precioEstandar = "";
      String precioTarifa = "";

      Opcrmopportunities opportunity = OBDal.getInstance().get(Opcrmopportunities.class,
          opportunitiesId);

      Product producto = OBDal.getInstance().get(Product.class, productId);
      PaymentTerm condicionPago = OBDal.getInstance().get(PaymentTerm.class,
          paymentTermId == "" ? condicionId : paymentTermId);
      FIN_PaymentMethod metodoPago = OBDal.getInstance().get(FIN_PaymentMethod.class, metodoId);
      PriceList listaPrecio = OBDal.getInstance().get(PriceList.class, priceList);

      OBCriteria<PriceListVersion> versionPriceList = OBDal.getInstance()
          .createCriteria(PriceListVersion.class);
      versionPriceList.add(Restrictions.eq(PriceListVersion.PROPERTY_PRICELIST, listaPrecio));
      versionPriceList.addOrderBy(PriceListVersion.PROPERTY_VALIDFROMDATE, false);
      versionPriceList.setFilterOnReadableOrganization(false);
      versionPriceList.setMaxResults(1);

      OBCriteria<QuotationOpportunity> proformaOBList = OBDal.getInstance()
          .createCriteria(QuotationOpportunity.class);
      proformaOBList
          .add(Restrictions.eq(QuotationOpportunity.PROPERTY_OPCRMOPPORTUNITIES, opportunity));

      OBCriteria<ProductPrice> productPriceList = OBDal.getInstance()
          .createCriteria(ProductPrice.class);
      productPriceList.add(Restrictions.eq(ProductPrice.PROPERTY_PRODUCT, producto));
      productPriceList.add(Restrictions.eq(ProductPrice.PROPERTY_PRICELISTVERSION,
          (PriceListVersion) versionPriceList.uniqueResult()));

      QuotationOpportunity proforma = OBProvider.getInstance().get(QuotationOpportunity.class);

      // Current org
      OBCriteria<svhreRegistration> registrationList = OBDal.getInstance()
          .createCriteria(svhreRegistration.class);
      registrationList.add(Restrictions.eq(svhreRegistration.PROPERTY_ACTIVE, true));
      registrationList.add(Restrictions.eq(svhreRegistration.PROPERTY_ORGANIZATION,
          OBContext.getOBContext().getCurrentOrganization()));
      registrationList.add(Restrictions.eq(svhreRegistration.PROPERTY_PRODUCT, producto));
      registrationList.setMaxResults(1);
      svhreRegistration registrationRow = (svhreRegistration) registrationList.uniqueResult();
      BigDecimal registration = BigDecimal.ZERO;
      if (registrationRow != null) {
        registration = registrationRow.getAmount();
      } else {
        // organization *
        OBCriteria<Organization> orgAll = OBDal.getInstance().createCriteria(Organization.class);
        orgAll.add(Restrictions.eq(Organization.PROPERTY_ID, "0"));
        orgAll.setMaxResults(1);
        Organization org = (Organization) orgAll.uniqueResult();
        if (org != null) {
          OBCriteria<svhreRegistration> registrationListOrgAll = OBDal.getInstance()
              .createCriteria(svhreRegistration.class);
          registrationListOrgAll.add(Restrictions.eq(svhreRegistration.PROPERTY_ACTIVE, true));
          registrationListOrgAll
              .add(Restrictions.eq(svhreRegistration.PROPERTY_ORGANIZATION + ".id", org.getId()));
          registrationListOrgAll
              .add(Restrictions.eq(svhreRegistration.PROPERTY_PRODUCT + ".id", producto.getId()));
          registrationListOrgAll.setMaxResults(1);
          svhreRegistration registrationOrgAllRow = (svhreRegistration) registrationListOrgAll
              .uniqueResult();
          if (registrationOrgAllRow != null) {
            registration = registrationOrgAllRow.getAmount();
          }
        } else {
          registration = BigDecimal.ZERO;
        }
      }

      ProductPrice productPriceAdd = null;
      for (ProductPrice productPrice : productPriceList.list()) {
        if (productPrice.getPriceListVersion().getPriceList().getId().equals(listaPrecio.getId())) {
          productPriceAdd = productPrice;
        }
      }

      if (productPriceAdd != null) {
        org.openbravo.model.common.order.Order temp = OBProvider.getInstance()
            .get(org.openbravo.model.common.order.Order.class);
        temp.setClient(opportunity.getClient());
        temp.setOrganization(opportunity.getOrganization());
        temp.setBusinessPartner(opportunity.getBusinessPartner());
        temp.setOrderDate(new Date());
        temp.setSalesCampaign(opportunity.getSalesCampaign());
        temp.setPriceList(listaPrecio);
        temp.setPaymentTerms(condicionPago);
        temp.setTransactionDocument(opportunity.getEcsfqDoctype());
        temp.setDocumentType(opportunity.getEcsfqDoctype());
        BusinessPartner tercero = OBDal.getInstance().get(BusinessPartner.class,
            opportunity.getBusinessPartner().getId());
        temp.setCurrency(tercero.getCurrency());
        if (temp.getCurrency() == null) {
          Organization organizacion = OBDal.getInstance().get(Organization.class,
              opportunity.getOrganization().getId());
          temp.setCurrency(organizacion.getCurrency());
        }

        /*
         * precioEstandar = ec.com.sidesoft.backoffice.discount.businessUtility.PriceAdjustment
         * .calculatePriceActual(temp, producto, new BigDecimal(1),
         * productPriceAdd.getStandardPrice()) .toString();
         */
        precioEstandar = getPrice(productPriceAdd.getStandardPrice(), opportunity, scale, producto)
            .toString();
        precioTarifa = productPriceAdd.getListPrice().toString();
      } else {
        precioEstandar = "0.00";
        precioTarifa = "0.00";
      }
      if (proformaOBList.count() > 0) {
        if (proformaOBList.list().get(0).getPaymentTerms().getOffsetMonthDue()
            .equals(condicionPago.getOffsetMonthDue())
            && proformaOBList.list().get(0).getPaymentTerms().getOverduePaymentDaysRule()
                .equals(condicionPago.getOverduePaymentDaysRule())) {
          proforma.setOpcrmOpportunities(opportunity);
          proforma.setPaymentTerms(condicionPago);
          proforma.setProduct(producto);
          proforma.setPaymentMethod(metodoPago);
          proforma.setPrecioe(precioEstandar);
          proforma.setPreciot(precioTarifa);
          proforma.setPriceList(listaPrecio);
          OBDal.getInstance().save(proforma);
          proforma.setEntrada(getEntrada(producto, condicionPago, new BigDecimal(entrada),
              new BigDecimal(precioEstandar), proforma, opportunity).toString());
          proforma.setDescuento(descuento);
          proforma
              .setCuota(getCreditFee(producto, condicionPago, proforma, opportunity).toString());
          proforma
              .setImpuesto(getTotalTax(proforma, condicionPago, producto, opportunity).toString());
          proforma.setTotal(getTotalCreditAmount(proforma, condicionPago).toString());
          proforma.setSvhreRegistrationAmt(registration.toString());
          proforma.setSvhreEntregAmt(getEntrada(producto, condicionPago, new BigDecimal(entrada),
              new BigDecimal(precioEstandar), proforma, opportunity).add(registration).toString());
        } else {
          return false;
        }
      } else {
        proforma.setOpcrmOpportunities(opportunity);
        proforma.setPaymentTerms(condicionPago);
        proforma.setProduct(producto);
        proforma.setPaymentMethod(metodoPago);
        proforma.setPrecioe(precioEstandar);
        proforma.setPriceList(listaPrecio);
        proforma.setPreciot(precioTarifa);
        OBDal.getInstance().save(proforma);
        proforma.setEntrada(getEntrada(producto, condicionPago, new BigDecimal(entrada),
            new BigDecimal(precioEstandar), proforma, opportunity).toString());
        proforma.setDescuento(descuento);
        proforma.setCuota(getCreditFee(producto, condicionPago, proforma, opportunity).toString());
        proforma
            .setImpuesto(getTotalTax(proforma, condicionPago, producto, opportunity).toString());
        proforma.setTotal(getTotalCreditAmount(proforma, condicionPago).toString());
        proforma.setSvhreRegistrationAmt(registration.toString());
        proforma.setSvhreEntregAmt(getEntrada(producto, condicionPago, new BigDecimal(entrada),
            new BigDecimal(precioEstandar), proforma, opportunity).add(registration).toString());
      }

      OBDal.getInstance().save(proforma);
      OBDal.getInstance().flush();
      SscfProfileCreditRisk profileCreditRisk = getProfileCreditRisConfiguration(producto,
          condicionPago, proforma, opportunity);
      if (profileCreditRisk == null & condicionPago.getOffsetMonthDue() > 0) {
        return false;
      }
      return true;
    } catch (Exception ex) {
      ex.printStackTrace();
      return false;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private String newPriceEstandar(QuotationOpportunity proforma, String opportunityId,
      PaymentTerm condicionPago) {
    String precioEstandar = "";
    try {
      OBContext.setAdminMode(true);

      Product producto = OBDal.getInstance().get(Product.class, proforma.getProduct().getId());
      PriceList listaPrecio = OBDal.getInstance().get(PriceList.class,
          proforma.getPriceList().getId());

      OBCriteria<PriceListVersion> versionPriceList = OBDal.getInstance()
          .createCriteria(PriceListVersion.class);
      versionPriceList.add(Restrictions.eq(PriceListVersion.PROPERTY_PRICELIST, listaPrecio));
      versionPriceList.addOrderBy(PriceListVersion.PROPERTY_VALIDFROMDATE, false);
      versionPriceList.setFilterOnReadableOrganization(false);
      versionPriceList.setMaxResults(1);

      OBCriteria<ProductPrice> productPriceList = OBDal.getInstance()
          .createCriteria(ProductPrice.class);
      productPriceList.add(Restrictions.eq(ProductPrice.PROPERTY_PRODUCT, producto));
      productPriceList.add(Restrictions.eq(ProductPrice.PROPERTY_PRICELISTVERSION,
          (PriceListVersion) versionPriceList.uniqueResult()));

      ProductPrice productPriceAdd = null;
      for (ProductPrice productPrice : productPriceList.list()) {
        if (productPrice.getPriceListVersion().getPriceList().getId().equals(listaPrecio.getId())) {
          productPriceAdd = productPrice;
        }
      }

      if (productPriceAdd != null) {
        Opcrmopportunities opportunity = OBDal.getInstance().get(Opcrmopportunities.class,
            opportunityId);
        org.openbravo.model.common.order.Order temp = OBProvider.getInstance()
            .get(org.openbravo.model.common.order.Order.class);
        temp.setClient(opportunity.getClient());
        temp.setOrganization(opportunity.getOrganization());
        temp.setBusinessPartner(opportunity.getBusinessPartner());
        temp.setOrderDate(new Date());
        temp.setSalesCampaign(opportunity.getSalesCampaign());
        temp.setPriceList(listaPrecio);
        temp.setPaymentTerms(condicionPago);
        BusinessPartner tercero = OBDal.getInstance().get(BusinessPartner.class,
            opportunity.getBusinessPartner().getId());
        temp.setCurrency(tercero.getCurrency());
        temp.setTransactionDocument(opportunity.getEcsfqDoctype());
        temp.setDocumentType(opportunity.getEcsfqDoctype());
        if (temp.getCurrency() == null) {
          Organization organizacion = OBDal.getInstance().get(Organization.class,
              opportunity.getOrganization().getId());
          temp.setCurrency(organizacion.getCurrency());
        }
        /*
         * precioEstandar = ec.com.sidesoft.backoffice.discount.businessUtility.PriceAdjustment
         * .calculatePriceActual(temp, producto, new BigDecimal(1),
         * productPriceAdd.getStandardPrice()) .toString();
         */
        precioEstandar = getPrice(productPriceAdd.getStandardPrice(), opportunity, scale, producto)
            .toString();
      } else {
        precioEstandar = "0.00";
      }
    } catch (Exception ex) {

    } finally {
      OBContext.restorePreviousMode();
    }
    return precioEstandar;
  }

  private void addQuotation(String opportunitiesId, String paymentTypeId) throws Exception {
    try {
      OBError msg = new OBError();
      OBContext.setAdminMode(true);

      Opcrmopportunities opportunity = OBDal.getInstance().get(Opcrmopportunities.class,
          opportunitiesId);
      Organization organizacion = OBDal.getInstance().get(Organization.class,
          opportunity.getOrganization().getId());
      OBCriteria<OrgWarehouse> crtAlmacen = OBDal.getInstance().createCriteria(OrgWarehouse.class);
      crtAlmacen.add(Restrictions.eq(OrgWarehouse.PROPERTY_ORGANIZATION, organizacion));
      BusinessPartner tercero = OBDal.getInstance().get(BusinessPartner.class,
          opportunity.getBusinessPartner().getId());
      BigDecimal entry = BigDecimal.ZERO;
      BigDecimal registration = BigDecimal.ZERO;
      BigDecimal entreg = BigDecimal.ZERO;

      OBCriteria<QuotationOpportunity> quotationList = OBDal.getInstance()
          .createCriteria(QuotationOpportunity.class);
      quotationList.add(Restrictions.eq(QuotationOpportunity.PROPERTY_ALERTSTATUS, true));
      quotationList
          .add(Restrictions.eq(QuotationOpportunity.PROPERTY_OPCRMOPPORTUNITIES, opportunity));

      for (QuotationOpportunity proforma : quotationList.list()) {
        entry = entry.add(new BigDecimal(proforma.getEntrada()));
        registration = registration.add(new BigDecimal(proforma.getSvhreRegistrationAmt()));
        entreg = entreg.add(new BigDecimal(proforma.getSvhreEntregAmt()));
      }
      ECSFQ_Quotation quotation = OBProvider.getInstance().get(ECSFQ_Quotation.class);
      quotation.setCurrency(tercero.getCurrency());
      quotation.setDocumentType(opportunity.getEcsfqDoctype());
      quotation.setTransactionDocument(opportunity.getEcsfqDoctype());
      if (quotation.getCurrency() == null) {
        quotation.setCurrency(organizacion.getCurrency());
      }
      quotation.setBusinessPartner(opportunity.getBusinessPartner());
      quotation.setOpcrmOpportunities(opportunity);
      quotation.setClient(opportunity.getClient());
      quotation.setOrganization(opportunity.getOrganization());
      quotation.setPriceList(quotationList.list().get(0).getPriceList());
      quotation.setOrderDate(new Date());
      quotation.setPaymentMethod(quotationList.list().get(0).getPaymentMethod());
      quotation.setPaymentTerms(quotationList.list().get(0).getPaymentTerms());
      quotation.setDocact("PR");
      quotation.setDocumentStatus("DR");
      quotation.setProcessed(false);
      quotation.setSscorProfile("ECSFQ_Quotation");
      quotation.setSalesCampaign(opportunity.getSalesCampaign());
      quotation.setSalesRepresentative(opportunity.getAssignedTo());
      quotation.setTransactionDocument(opportunity.getEcsfqDoctype());
      quotation.setDocumentType(opportunity.getEcsfqDoctype());
      quotation.setCurrency(opportunity.getCurrency());
      quotation
          .setPaymenttype(quotationList.list().get(0).getPaymentTerms().getSssovlPaymenttype());
      quotation.setEntry(entry);
      quotation.setSvhreRegistrationAmt(registration);
      quotation.setSvhreEntregAmt(entreg);
      quotation.setSvhreRegPendingAmt(registration);

      if (crtAlmacen.count() > 0) {
        crtAlmacen.addOrder(Order.asc(OrgWarehouse.PROPERTY_PRIORITY));
        List<OrgWarehouse> warehouse = crtAlmacen.list();
        quotation.setWarehouse(warehouse.get(0).getWarehouse());
      }
      OBDal.getInstance().save(quotation);
      OBDal.getInstance().flush();
      addLineQuotation(quotation, opportunity);
      if (quotation.getPaymentTerms().getOffsetMonthDue() > 0) {
        // addAmortization(quotation,opportunity);
        ConnectionProvider conn = new DalConnectionProvider(false);
        try {
          String sql = "select SSCOR_RECALCULATE0(null, ?, 'A');";
          PreparedStatement st = null;
          st = conn.getPreparedStatement(sql);
          st.setString(1, quotation.getId());
          ResultSet resultSet = st.executeQuery();
        } catch (Exception e) {
          System.out.println("No se genero la tabla de amortizacion");
        } finally {
          conn.destroy();
        }
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      throw ex;
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  private void addLineQuotation(ECSFQ_Quotation quotation, Opcrmopportunities opportunity)
      throws Exception {
    try {
      OBContext.setAdminMode(true);
      SLOrderAmtData[] data = SLOrderAmtData.select(this, quotation.getPriceList().getId());
      int stdPrecision = Integer.valueOf(data[0].stdprecision);
      int pricePrecision = Integer.valueOf(data[0].priceprecision);
      OBError msg = new OBError();
      BigDecimal total = BigDecimal.ZERO;
      BigDecimal totalTax = BigDecimal.ZERO;
      BigDecimal totalToPaid = BigDecimal.ZERO;
      BigDecimal entry = BigDecimal.ZERO;

      OBCriteria<QuotationOpportunity> quotationList = OBDal.getInstance()
          .createCriteria(QuotationOpportunity.class);
      quotationList.add(Restrictions.eq(QuotationOpportunity.PROPERTY_ALERTSTATUS, true));
      quotationList
          .add(Restrictions.eq(QuotationOpportunity.PROPERTY_OPCRMOPPORTUNITIES, opportunity));

      for (QuotationOpportunity proforma : quotationList.list()) {
	      entry = new BigDecimal(proforma.getEntrada());
        QuotationLineTax linetax = OBProvider.getInstance().get(QuotationLineTax.class);
        Product product = OBDal.getInstance().get(Product.class, proforma.getProduct().getId());
        PaymentTerm terminos = OBDal.getInstance().get(PaymentTerm.class,
            proforma.getPaymentTerms().getId());
        BigDecimal totalLine = getTotalCreditAmountWithoutInterest(product, proforma, terminos,
            opportunity).setScale(pricePrecision, RoundingMode.HALF_UP);
        BigDecimal impuestoLine = new BigDecimal(proforma.getImpuesto()).setScale(pricePrecision,
            RoundingMode.HALF_UP);
        BigDecimal Total = new BigDecimal(proforma.getTotal()).setScale(pricePrecision,
            RoundingMode.HALF_UP);
        QuotationLine lineQuotation = OBProvider.getInstance().get(QuotationLine.class);
        lineQuotation.setOrganization(quotation.getOrganization());
        lineQuotation.setClient(quotation.getClient());
        lineQuotation.setEcsfqOrder(quotation);
        lineQuotation.setLineNo(getSequenceNumber(quotation));
        lineQuotation.setProduct(proforma.getProduct());
        lineQuotation.setOrderedQuantity(new Long(1));
        lineQuotation.setListPrice(totalLine.subtract(impuestoLine));
        lineQuotation.setDiscount(BigDecimal.ZERO);
        lineQuotation.setUOM(product.getUOM());
        lineQuotation.setLineNetAmount(totalLine.subtract(impuestoLine));
        lineQuotation.setUnitPrice(totalLine.subtract(impuestoLine));
        lineQuotation.setWarehouse(quotation.getWarehouse());
        lineQuotation.setEntry(entry.longValue());
        totalTax = totalTax.add(totalLine);
        total = total.add(totalLine.subtract(impuestoLine));
        totalToPaid = totalToPaid.add(Total);

        TaxCategory taxCategory = proforma.getProduct().getTaxCategory();
        OBCriteria<TaxRate> crtTaxRate = OBDal.getInstance().createCriteria(TaxRate.class);
        crtTaxRate.add(Restrictions.eq(TaxRate.PROPERTY_TAXCATEGORY, taxCategory));
        crtTaxRate.add(Restrictions.eq(TaxRate.PROPERTY_DEFAULT, true));
        if (crtTaxRate.count() > 0) {
          for (TaxRate taxRateE : crtTaxRate.list()) {
            lineQuotation.setTax(taxRateE);
            linetax.setTax(taxRateE);
          }
        }

        OBDal.getInstance().save(lineQuotation);
        OBDal.getInstance().flush();

        List<QuotationOpportunity> tempProforma = new ArrayList<QuotationOpportunity>();
        tempProforma.add(proforma);
        // List<Promotion> promociones = Promotions.getallPromotion(tempProforma,
        // quotation.getOpcrmOpportunities().getId());
        OBCriteria<Promotion> promotionList = OBDal.getInstance().createCriteria(Promotion.class);
        promotionList.add(Restrictions.eq(Promotion.PROPERTY_ECSFQQUOTATION, proforma));
        promotionList.add(Restrictions.eq(Promotion.PROPERTY_ALERTSTATUS, true));

        for (Promotion promocion : promotionList.list()) {
          PriceAdjustment priceAdj = promocion.getPromotionDiscount();
          EcsfqPromoLine promoline = OBProvider.getInstance().get(EcsfqPromoLine.class);
          promoline.setClient(lineQuotation.getClient());
          promoline.setOrganization(lineQuotation.getOrganization());
          promoline.setEcsfqOrderline(lineQuotation);
          promoline.setOffer(priceAdj);
          OBDal.getInstance().save(promoline);
          OBDal.getInstance().flush();
        }

        linetax.setEcsfqOrderline(lineQuotation);
        linetax.setOrganization(quotation.getOrganization());
        linetax.setClient(quotation.getClient());
        linetax.setTaxAmount((new BigDecimal(proforma.getImpuesto())));
        linetax.setTaxableAmount(((new BigDecimal(proforma.getTotal()))
            .subtract(new BigDecimal(proforma.getImpuesto()))));

        OBDal.getInstance().save(linetax);
        OBDal.getInstance().flush();

      }

      ECSFQ_Quotation quotationTmp = OBDal.getInstance().get(ECSFQ_Quotation.class,
          quotation.getId());
      quotationTmp.setGrandTotalAmount(totalToPaid);
      quotationTmp.setSummedLineAmount(totalToPaid.subtract(totalTax.subtract(total)));
      OBDal.getInstance().save(quotationTmp);
      OBDal.getInstance().flush();
    } catch (Exception ex) {
      ex.printStackTrace();
      throw ex;
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  public Long getSequenceNumber(ECSFQ_Quotation quotation) throws OBException {
    OBCriteria<QuotationLine> obc = OBDal.getInstance().createCriteria(QuotationLine.class);
    obc.add(Restrictions.eq(QuotationLine.PROPERTY_ECSFQORDER, quotation));
    obc.addOrderBy(QuotationLine.PROPERTY_LINENO, false);
    obc.setFilterOnReadableOrganization(false);
    obc.setMaxResults(1);
    QuotationLine attach = (QuotationLine) obc.uniqueResult();
    if (attach == null) {
      return 10L;
    }
    return attach.getLineNo() + 10L;
  }

  private void printPage(HttpServletResponse response, VariablesSecureApp vars, String strOrgId,
      String strWindowId, String strTabId, String opportunityId, String strStatementDate,
      String strEndBalance, boolean status, String action, String paymentTermId,
      String paymentTypeId, String promociones) throws IOException, ServletException {

    String dateFormat = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("dateFormat.java");
    SimpleDateFormat dateFormater = new SimpleDateFormat(dateFormat);

    Opcrmopportunities opportunity = OBDal.getInstance().get(Opcrmopportunities.class,
        opportunityId);

    OBCriteria<QuotationOpportunity> quotationList = OBDal.getInstance()
        .createCriteria(QuotationOpportunity.class);
    quotationList
        .add(Restrictions.eq(QuotationOpportunity.PROPERTY_OPCRMOPPORTUNITIES, opportunity));

    try {
      OBContext.setAdminMode();
      XmlDocument xmlDocument = xmlEngine
          .readXmlTemplate("ec/com/sidesoft/fast/quotation/ad_action/Quotation")
          .createXmlDocument();

      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
      xmlDocument.setParameter("theme", vars.getTheme());

      xmlDocument.setParameter("dateDisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
      xmlDocument.setParameter("mainDate", DateTimeData.today(this));
      xmlDocument.setParameter("windowId", strWindowId);
      xmlDocument.setParameter("tabId", strTabId);
      xmlDocument.setParameter("orgId", strOrgId);
      xmlDocument.setParameter("opportunityId", opportunityId);
      xmlDocument.setParameter("showGridOption", "N");

      for (QuotationOpportunity productQuotation : quotationList.list()) {
        if (productQuotation.getProduct() != null
            && productQuotation.getProduct().getSssifldSswosClass() != null
            && productQuotation.getProduct().getSssifldSswosClass().isSupcIsvisibleQuotation()) {
          xmlDocument.setParameter("showGridOption", "Y");
        }
      }

      if (quotationList.count() > 0) {
        xmlDocument.setParameter("cbxTermDefault", paymentTermId);
        xmlDocument.setParameter("cbxTermTypeDefault", paymentTypeId);
        xmlDocument.setParameter("countProforma", String.valueOf(quotationList.count()));
      } else {
        xmlDocument.setParameter("cbxTermDefault", "");
        xmlDocument.setParameter("cbxTermTypeDefault", "");
        xmlDocument.setParameter("countProforma", "");
      }

      BigDecimal currentEndBalance = BigDecimal.ZERO;
      if (vars.commandIn("PROCESS")) {
        xmlDocument.setParameter("statementDate", strStatementDate);
        xmlDocument.setParameter("endBalance", strEndBalance);
        xmlDocument.setParameter("calcEndingBalance", strEndBalance);

      } else {
        String currentStatementDate = DateTimeData.today(this);
        xmlDocument.setParameter("statementDate", currentStatementDate);
        xmlDocument.setParameter("endBalance", null);
        xmlDocument.setParameter("calcEndingBalance", null);
      }

      BigDecimal beginBalance = BigDecimal.ZERO;

      xmlDocument.setParameter("account", "");
      xmlDocument.setParameter("beginBalance", "");
      xmlDocument.setParameter("paramPromotions", promociones);
      try {
        // 691A2B8F248446DDA17F504EA29CBA49 id de la referencia clave de la lista -->
        // Sssovl_PaymentType
        ComboTableData comboTableData = new ComboTableData(vars, this, "LIST", "",
            "691A2B8F248446DDA17F504EA29CBA49", "", Utility.getContext(this, vars, "#User_Org", ""),
            Utility.getContext(this, vars, "#User_Client", ""), 0);
        Utility.fillSQLParameters(this, vars, null, comboTableData, "", "100");
        xmlDocument.setData("report_PaymentType", "liststructure", comboTableData.select(false));
        comboTableData = null;
      } catch (Exception ex) {
        throw new ServletException(ex);
      }

      try {
        // 2CB89E3A76C24521AD2B71170D24BE19 id de la regla de validacion --> PricingPriceList FROM
        // SALES
        ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "M_PriceList_ID",
            "", "2CB89E3A76C24521AD2B71170D24BE19", Utility.getContext(this, vars, "#User_Org", ""),
            Utility.getContext(this, vars, "#User_Client", ""), 0);
        Utility.fillSQLParameters(this, vars, null, comboTableData, "", "100");
        xmlDocument.setData("reportAD_Org_ID", "liststructure", comboTableData.select(false));
        comboTableData = null;
      } catch (Exception ex) {
        throw new ServletException(ex);
      }

      try {
        // 826EDE57DC33471AA6D86883DC55F9ED id de la regla de validacion --> PaymentTerm FQ
        ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR",
            "C_PaymentTerm_ID", "", "826EDE57DC33471AA6D86883DC55F9ED",
            Utility.getContext(this, vars, "#User_Org", "RequisitionToOrder"),
            Utility.getContext(this, vars, "#User_Client", "RequisitionToOrder"), 0);
        Utility.fillSQLParameters(this, vars, null, comboTableData, "RequisitionToOrder",
            paymentTermId);
        xmlDocument.setData("reportAD_O", "liststructure", comboTableData.select(false));
        // xmlDocument.setData("reportAD_O", "liststructure",
        // PaymentType.getTransactionsFiltered("Test"));
        comboTableData = null;
      } catch (Exception ex) {
        throw new ServletException(ex);
      }

      try {
        ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR",
            "FIN_Paymentmethod_ID", "", "",
            Utility.getContext(this, vars, "#User_Org", "RequisitionToOrder"),
            Utility.getContext(this, vars, "#User_Client", "RequisitionToOrder"), 0);
        Utility.fillSQLParameters(this, vars, null, comboTableData, "RequisitionToOrder", "");
        xmlDocument.setData("reportAD_Payments", "liststructure", comboTableData.select(false));
        comboTableData = null;
      } catch (Exception ex) {
        throw new ServletException(ex);
      }

      BigDecimal totalPayment = BigDecimal.ZERO;
      BigDecimal totalDeposit = BigDecimal.ZERO;
      BigDecimal totalCuota = BigDecimal.ZERO;
      BigDecimal totalTax = BigDecimal.ZERO;
      BigDecimal total = BigDecimal.ZERO;
      BigDecimal totalRegistration = BigDecimal.ZERO;
      BigDecimal totalEM = BigDecimal.ZERO;
      Long noCuotas = 0L;
      Long noCuota = 0L;
      if (quotationList.count() <= 0) {
        xmlDocument.setParameter("calcTotalNoCuota", "0");
        xmlDocument.setParameter("calcTotalDescuento", "0.00");
        xmlDocument.setParameter("calcTotalEntrada", "0.00");
        xmlDocument.setParameter("calcTotalRegistration", "0.00");
        xmlDocument.setParameter("calcTotalEM", "0.00");
        xmlDocument.setParameter("caclTotalCuota", "0.00");
        xmlDocument.setParameter("caclTotalImp", "0.00");
        xmlDocument.setParameter("caclTotal", "0.00");

      } else {

        for (QuotationOpportunity fp : quotationList.list()) {
          BigDecimal payAmt = new BigDecimal(fp.getEntrada());
          BigDecimal depAmt = new BigDecimal(fp.getDescuento());
          BigDecimal cuotaAmt = new BigDecimal(fp.getCuota());
          BigDecimal depTotal = new BigDecimal(fp.getTotal());
          BigDecimal depTax = new BigDecimal(fp.getImpuesto());

          // Inicia nuevas columnas
          BigDecimal regAmt = new BigDecimal(fp.getSvhreRegistrationAmt());
          BigDecimal EMAmt = new BigDecimal(fp.getSvhreEntregAmt());

          totalPayment = totalPayment.add(payAmt);
          totalDeposit = totalDeposit.add(depAmt);
          totalCuota = totalCuota.add(cuotaAmt);
          noCuota = fp.getPaymentTerms().getOffsetMonthDue();
          noCuotas = fp.getPaymentTerms().getOffsetMonthDue();
          total = total.add(depTotal);
          totalTax = totalTax.add(depTax);

          // Nuevas columnas
          totalRegistration = totalRegistration.add(regAmt);
          totalEM = totalEM.add(EMAmt);

        }
        xmlDocument.setParameter("calcTotalNoCuota", Long.toString(noCuotas));
        xmlDocument.setParameter("calcTotalCuotas", Long.toString(noCuota));
        xmlDocument.setParameter("caclTotalCuota", totalCuota.toString());

        xmlDocument.setParameter("calcTotalDescuento", totalDeposit.toString());
        xmlDocument.setParameter("calcTotalEntrada", totalPayment.toString());
        // Nuevas columnas
        xmlDocument.setParameter("calcTotalRegistration", totalRegistration.toString());
        xmlDocument.setParameter("calcTotalEM", totalEM.toString());

        xmlDocument.setParameter("caclTotalImp", totalTax.toString());
        xmlDocument.setParameter("caclTotal", total.toString());

      }
      // Hidden inputs
      xmlDocument.setParameter("calcBeginningBalance", beginBalance.toString());
      xmlDocument.setParameter("calcTotalPayment", BigDecimal.ZERO.toString());
      xmlDocument.setParameter("calcTotalDeposit", BigDecimal.ZERO.toString());
      xmlDocument.setParameter("calcDifferenceToClear",
          currentEndBalance.subtract(beginBalance).toString());
      xmlDocument.setParameter("calcCurrentlyCleared", "0.00");
      xmlDocument.setParameter("calcDifference", "0.00");
      xmlDocument.setParameter("precision", "0.00");

      // OBError msg = new OBError();;
      OBError myMessage = vars.getMessage(strWindowId);

      if (!status && action.equals("A")) {
        msg.setType("Error");
        msg.setTitle(Utility.messageBD(this, "Info", vars.getLanguage()));
        msg.setMessage(
            Utility.parseTranslation(this, vars, vars.getLanguage(), "@ecsfq_payment_invalid@"));
        myMessage = msg;
      } else if (!status && action.equals("PCR")) {
        msg = new OBError();
        msg.setType("Error");
        msg.setTitle(Utility.messageBD(this, "Info", vars.getLanguage()));
        msg.setMessage(Utility.parseTranslation(this, vars, vars.getLanguage(),
            "@ecsfq_configuration_error@"));
        myMessage = msg;
      }

      vars.removeMessage(strWindowId);
      if (myMessage != null) {
        xmlDocument.setParameter("messageType", myMessage.getType());
        xmlDocument.setParameter("messageTitle", myMessage.getTitle());
        xmlDocument.setParameter("messageMessage", myMessage.getMessage());
      }

      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      out.println(xmlDocument.print());
      out.close();
    } catch (Exception e) {
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void reloadPaymentTypeCombo(HttpServletResponse response, String srtPaymentMethod,
      String paymentTermId) throws IOException, ServletException {
    log4j.debug("Callout: Financial Account has changed to");

    String paymentTypeComboHtml = PaymentType.getPaymentTypeList(srtPaymentMethod, paymentTermId);

    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(paymentTypeComboHtml.replaceAll("\"", "\\'"));
    out.close();
  }

  private void printGrid(HttpServletResponse response, String opportunitiesId)
      throws IOException, ServletException {

    XmlDocument xmlDocument = xmlEngine
        .readXmlTemplate("ec/com/sidesoft/fast/quotation/ad_action/QuotationGrid")
        .createXmlDocument();

    Opcrmopportunities opportunity = OBDal.getInstance().get(Opcrmopportunities.class,
        opportunitiesId);

    OBCriteria<QuotationOpportunity> quotationList = OBDal.getInstance()
        .createCriteria(QuotationOpportunity.class);
    quotationList
        .add(Restrictions.eq(QuotationOpportunity.PROPERTY_OPCRMOPPORTUNITIES, opportunity));

    FieldProvider[] data = MethodsDao.getTransactionsFiltered(quotationList.list());

    xmlDocument.setData("structure", data);

    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  private void printSecondGrid(HttpServletResponse response, String opportunitiesId)
      throws IOException, ServletException {

    XmlDocument xmlDocument = xmlEngine
        .readXmlTemplate("ec/com/sidesoft/fast/quotation/ad_action/PromotionGrid")
        .createXmlDocument();

    Opcrmopportunities opportunity = OBDal.getInstance().get(Opcrmopportunities.class,
        opportunitiesId);
    OBCriteria<QuotationOpportunity> quotationListCheck = OBDal.getInstance()
        .createCriteria(QuotationOpportunity.class);
    quotationListCheck
        .add(Restrictions.eq(QuotationOpportunity.PROPERTY_OPCRMOPPORTUNITIES, opportunity));
    quotationListCheck.add(Restrictions.eq(QuotationOpportunity.PROPERTY_ALERTSTATUS, true));
    quotationListCheck.add(Restrictions.eq(QuotationOpportunity.PROPERTY_ISPROCESS, true));

    List<QuotationOpportunity> quotations = quotationListCheck.list();
    if (quotationListCheck.count() > 0) {
      quotations.clear();
      quotations = quotationListCheck.list();
    } else {
      quotations.clear();
      OBCriteria<QuotationOpportunity> quotationList = OBDal.getInstance()
          .createCriteria(QuotationOpportunity.class);
      quotationList
          .add(Restrictions.eq(QuotationOpportunity.PROPERTY_OPCRMOPPORTUNITIES, opportunity));
      quotationListCheck.add(Restrictions.eq(QuotationOpportunity.PROPERTY_ISPROCESS, true));
      quotations = quotationList.list();
    }

    List<Promotion> promotionList = new ArrayList<Promotion>();

    if (quotations.size() > 0) {
      OBCriteria<Promotion> promotion = OBDal.getInstance().createCriteria(Promotion.class);
      promotion.add(Restrictions.in(Promotion.PROPERTY_ECSFQQUOTATION, quotations));
      promotion.add(Restrictions.eq(Promotion.PROPERTY_ALERTSTATUS, true));

      promotionList = promotion.list();
    }

    // promotionList.clear();
    FieldProvider[] dataPromotions = MethodsDao.getPromotionsFiltered(promotionList);

    xmlDocument.setData("structure2", dataPromotions);

    response.setContentType("text/html; charset=UTF-8");
    // removePromotions(opportunitiesId);
    quotations.clear();
    // promotionList.clear();

    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  public BigDecimal getCreditFee(Product product, PaymentTerm term, QuotationOpportunity proforma,
      Opcrmopportunities opportunity) throws NullPointerException {
    if (term.getOffsetMonthDue() > 0) {
      try {
        OBContext.setAdminMode(true);
        BigDecimal tax = getTaxValue(product).divide(new BigDecimal(100), scale,
            RoundingMode.CEILING);
        SscfProfileCreditRisk profileCreditRisk = getProfileCreditRisConfiguration(product, term,
            proforma, opportunity);
        if (profileCreditRisk == null) {
          return BigDecimal.ZERO;
        }
        BigDecimal interesPrestamo = profileCreditRisk.getInterest()
            .divide(new BigDecimal(100), scale, RoundingMode.CEILING)
            .divide(new BigDecimal(12), scale, RoundingMode.CEILING);
        BigDecimal meses = new BigDecimal(term.getOffsetMonthDue());
        BigDecimal precio = new BigDecimal(proforma.getPrecioe());
        BigDecimal recargoMensual = profileCreditRisk.getRisk().divide(new BigDecimal(100), scale,
            RoundingMode.CEILING);
        BigDecimal entrada = getEntrada(product, term, new BigDecimal(proforma.getEntrada()),
            precio, proforma, opportunity, false);
        BigDecimal recargo = (precio.subtract(entrada)).multiply(meses).multiply(recargoMensual);
        BigDecimal monto = ((precio.add(recargo)).multiply(tax))
            .subtract(new BigDecimal(proforma.getEntrada())).add(precio.add(recargo));
        BigDecimal porcentajeCuota;
        try {
          porcentajeCuota = interesPrestamo.add(
              interesPrestamo.divide(((interesPrestamo.add(BigDecimal.ONE)).pow(meses.intValue()))
                  .subtract(new BigDecimal(1)), RoundingMode.CEILING));
        } catch (ArithmeticException e) {
          porcentajeCuota = BigDecimal.ONE.divide(meses, scale, RoundingMode.CEILING);
        }
        BigDecimal cuota = porcentajeCuota.multiply(monto);
        return cuota;
      } catch (Exception ex) {
        ex.printStackTrace();
      } finally {
        OBContext.restorePreviousMode();
      }
    }
    return BigDecimal.ZERO;
  }

  public BigDecimal getTotalCreditAmountWithoutInterest(Product product,
      QuotationOpportunity proforma, PaymentTerm term, Opcrmopportunities opportunity) {
    if (term.getOffsetMonthDue() > 0) {
      BigDecimal tax = getTaxValue(product).divide(new BigDecimal(100), scale,
          RoundingMode.CEILING);
      SscfProfileCreditRisk profileCreditRisk = getProfileCreditRisConfiguration(product, term,
          proforma, opportunity);
      if (profileCreditRisk == null) {
        return BigDecimal.ZERO;
      }
      BigDecimal meses = new BigDecimal(term.getOffsetMonthDue());
      BigDecimal precio = new BigDecimal(proforma.getPrecioe());
      BigDecimal recargoMensual = profileCreditRisk.getRisk().divide(new BigDecimal(100), scale,
          RoundingMode.CEILING);
      BigDecimal entrada = getEntrada(product, term, new BigDecimal(proforma.getEntrada()), precio,
          proforma, opportunity, false);
      BigDecimal recargo = (precio.subtract(entrada)).multiply(meses).multiply(recargoMensual);
      BigDecimal monto = ((precio.add(recargo)).multiply(tax)).add(precio.add(recargo));
      return monto;
    }
    return new BigDecimal(proforma.getPrecioe()).add(new BigDecimal(proforma.getImpuesto()));
  }

  public BigDecimal getTotalCreditAmount(QuotationOpportunity proforma, PaymentTerm term) {
    if (term.getOffsetMonthDue() > 0) {
      return (new BigDecimal(proforma.getCuota())
          .multiply(new BigDecimal(term.getOffsetMonthDue())))
              .add(new BigDecimal(proforma.getEntrada()));
    }
    return new BigDecimal(proforma.getPrecioe()).add(new BigDecimal(proforma.getImpuesto()));
  }

  public BigDecimal getTotalTax(QuotationOpportunity proforma, PaymentTerm term, Product product,
      Opcrmopportunities opportunity) {
    BigDecimal tax = getTaxValue(product).divide(new BigDecimal(100), scale, RoundingMode.CEILING);
    if (term.getOffsetMonthDue() > 0) {
      BigDecimal total = getTotalCreditAmountWithoutInterest(product, proforma, term, opportunity);
      BigDecimal taxComplete = tax.add(BigDecimal.ONE);
      return total.subtract(total.divide(taxComplete, scale, RoundingMode.CEILING));
    }
    return new BigDecimal(proforma.getPrecioe()).multiply(tax);
  }

  public SscfProfileCreditRisk getProfileCreditRisConfiguration(Product product, PaymentTerm term,
      QuotationOpportunity proforma, Opcrmopportunities opportunity) {
    if (term.getOffsetMonthDue() > 0) {
      try {

        OBContext.setAdminMode(true);
        OBCriteria<SscfCreditRange> ctrCreditRange = OBDal.getInstance()
            .createCriteria(SscfCreditRange.class);
        ctrCreditRange.add(Restrictions.le(SscfCreditRange.PROPERTY_AMOUNTFROM,
            new BigDecimal(proforma.getPrecioe())));
        ctrCreditRange.add(Restrictions.ge(SscfCreditRange.PROPERTY_AMOUNTTO,
            new BigDecimal(proforma.getPrecioe())));
        if (ctrCreditRange.count() > 0) {

          /* Buscar promociones */
          OBCriteria<Promotion> promotion = OBDal.getInstance().createCriteria(Promotion.class);
          promotion.add(Restrictions.eq(Promotion.PROPERTY_ECSFQQUOTATION, proforma));
          promotion.add(Restrictions.eq(Promotion.PROPERTY_ALERTSTATUS, true));
          promotion.add(Restrictions.eq(Promotion.PROPERTY_PRODUCT, product));
          if (promotion.count() > 0) {
            for (Promotion promo : promotion.list()) {
              try {
                if (term.getOffsetMonthDue() <= promo.getPromotionDiscount().getEcsfqMonthsTo()
                    & term.getOffsetMonthDue() >= promo.getPromotionDiscount()
                        .getEcsfqMonthsFrom()) {
                  if (promo.getPromotionDiscount().getDiscountType().getId()
                      .equals("D4BE0D0868A841118FFC5D17B04DD689")) {
                    SscfProfileCreditRisk pcr = OBProvider.getInstance()
                        .get(SscfProfileCreditRisk.class);
                    pcr.setInputPercentage(promo.getPromotionDiscount().getEcsfqInput());
                    pcr.setRisk(promo.getPromotionDiscount().getEcsfqRisk());
                    pcr.setInterest(promo.getPromotionDiscount().getEcsfqInterest());
                    return pcr;
                  }
                }
              } catch (NullPointerException ex) {

              }
            }
          }

          /* Buscar por tercero */
          OBCriteria<ECSFQSpecialcondition> ctrSpecialConditionBp = OBDal.getInstance()
              .createCriteria(ECSFQSpecialcondition.class);
          ctrSpecialConditionBp.add(Restrictions.le(ECSFQSpecialcondition.PROPERTY_MINIMUIMTERM,
              term.getOffsetMonthDue()));
          ctrSpecialConditionBp.add(
              Restrictions.ge(ECSFQSpecialcondition.PROPERTY_DEADLINE, term.getOffsetMonthDue()));
          ctrSpecialConditionBp
              .add(Restrictions.eq(ECSFQSpecialcondition.PROPERTY_PROFILE, "ECSFQ_Quotation"));
          ctrSpecialConditionBp.add(Restrictions.in(ECSFQSpecialcondition.PROPERTY_SSCFCREDITRANGE,
              ctrCreditRange.list()));
          ctrSpecialConditionBp.add(Restrictions.eq(ECSFQSpecialcondition.PROPERTY_BPARTNER,
              opportunity.getBusinessPartner()));
          if (ctrSpecialConditionBp.count() > 0) {
            for (ECSFQSpecialcondition temp : ctrSpecialConditionBp.list()) {
              SscfProfileCreditRisk pcr = OBProvider.getInstance().get(SscfProfileCreditRisk.class);
              pcr.setInputPercentage(temp.getEntry());
              pcr.setRisk(temp.getRisk());
              pcr.setInterest(temp.getInterest());
              return pcr;
            }
          }

          /* Buscar por grupo tercero */
          OBCriteria<ECSFQSpecialcondition> ctrSpecialConditionBpg = OBDal.getInstance()
              .createCriteria(ECSFQSpecialcondition.class);
          ctrSpecialConditionBpg.add(Restrictions.le(ECSFQSpecialcondition.PROPERTY_MINIMUIMTERM,
              term.getOffsetMonthDue()));
          ctrSpecialConditionBpg.add(
              Restrictions.ge(ECSFQSpecialcondition.PROPERTY_DEADLINE, term.getOffsetMonthDue()));
          ctrSpecialConditionBpg
              .add(Restrictions.eq(ECSFQSpecialcondition.PROPERTY_PROFILE, "ECSFQ_Quotation"));
          ctrSpecialConditionBpg.add(Restrictions.in(ECSFQSpecialcondition.PROPERTY_SSCFCREDITRANGE,
              ctrCreditRange.list()));
          ctrSpecialConditionBpg.add(Restrictions.eq(ECSFQSpecialcondition.PROPERTY_BPGROUP,
              opportunity.getBusinessPartner().getBusinessPartnerCategory()));
          if (ctrSpecialConditionBpg.count() > 0) {
            for (ECSFQSpecialcondition temp : ctrSpecialConditionBpg.list()) {
              SscfProfileCreditRisk pcr = OBProvider.getInstance().get(SscfProfileCreditRisk.class);
              pcr.setInputPercentage(temp.getEntry());
              pcr.setRisk(temp.getRisk());
              pcr.setInterest(temp.getInterest());
              return pcr;
            }
          }

          /* Buscar por producto */
          OBCriteria<SscfProfileCreditRisk> crtProfileCreditRisk = OBDal.getInstance()
              .createCriteria(SscfProfileCreditRisk.class);
          crtProfileCreditRisk.add(Restrictions.eq(SscfProfileCreditRisk.PROPERTY_PRODUCTCATEGORY,
              product.getProductCategory()));
          crtProfileCreditRisk.add(Restrictions.le(SscfProfileCreditRisk.PROPERTY_MINIMUMTERM,
              term.getOffsetMonthDue()));
          crtProfileCreditRisk.add(Restrictions.ge(SscfProfileCreditRisk.PROPERTY_MAXIMUMTERM,
              term.getOffsetMonthDue()));
          crtProfileCreditRisk
              .add(Restrictions.eq(SscfProfileCreditRisk.PROPERTY_CUSTOMERTYPE, "R"));
          crtProfileCreditRisk
              .add(Restrictions.eq(SscfProfileCreditRisk.PROPERTY_PROFILE, "ECSFQ_Quotation"));
          crtProfileCreditRisk.add(Restrictions.in(SscfProfileCreditRisk.PROPERTY_SSCFCREDITRANGE,
              ctrCreditRange.list()));
          if (crtProfileCreditRisk.count() > 0) {
            for (SscfProfileCreditRisk temp : crtProfileCreditRisk.list()) {
              return temp;
            }
          }
        }
        state = "PCR";
        status = false;
      } catch (Exception ex) {
        ex.printStackTrace();
      } finally {
        OBContext.restorePreviousMode();
      }
    }
    return null;
  }

  public BigDecimal getTaxValue(Product product) {
    try {
      OBContext.setAdminMode(true);
      TaxCategory taxCategory = product.getTaxCategory();
      OBCriteria<TaxRate> crtTaxRate = OBDal.getInstance().createCriteria(TaxRate.class);
      crtTaxRate.add(Restrictions.eq(TaxRate.PROPERTY_TAXCATEGORY, taxCategory));
      crtTaxRate.add(Restrictions.eq(TaxRate.PROPERTY_DEFAULT, true));
      if (crtTaxRate.count() > 0) {
        for (TaxRate taxRateE : crtTaxRate.list()) {
          return taxRateE.getRate();
        }
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    } finally {
      OBContext.restorePreviousMode();
    }
    return BigDecimal.ZERO;
  }

  public BigDecimal getEntrada(Product product, PaymentTerm term, BigDecimal entrada,
      BigDecimal productPrice, QuotationOpportunity proforma, Opcrmopportunities opportunity) {
    return getEntrada(product, term, entrada, productPrice, proforma, opportunity, true);
  }

  public BigDecimal getEntrada(Product product, PaymentTerm term, BigDecimal entrada,
      BigDecimal productPrice, QuotationOpportunity proforma, Opcrmopportunities opportunity,
      boolean coniva) {
    if (term.getOffsetMonthDue() > 0) {
      SscfProfileCreditRisk profileCreditRisk = getProfileCreditRisConfiguration(product, term,
          proforma, opportunity);
      if (profileCreditRisk == null) {
        return BigDecimal.ZERO;
      }
      BigDecimal tax = getTaxValue(product).divide(new BigDecimal(100), scale,
          RoundingMode.CEILING);
      BigDecimal precio = productPrice.add(productPrice.multiply(tax));
      BigDecimal entradaMin = profileCreditRisk.getInputPercentage().compareTo(BigDecimal.ZERO) == 0
          ? BigDecimal.ZERO
          : profileCreditRisk.getInputPercentage().multiply(precio).divide(new BigDecimal(100),
              scale, RoundingMode.CEILING);
      if (entrada.compareTo(entradaMin) < 0) {
        if (coniva)
          return entradaMin;
        else
          return profileCreditRisk.getInputPercentage().multiply(productPrice)
              .divide(new BigDecimal(100), scale, RoundingMode.CEILING);
      }
      if (entradaMin.compareTo(BigDecimal.ZERO) == 0) {
        return BigDecimal.ZERO;
      } else {
        if (coniva) {
          return entrada;
        } else {
          BigDecimal taxComplete = tax.add(BigDecimal.ONE);
          return entrada.divide(taxComplete, scale, RoundingMode.CEILING);
        }
      }
    }
    return BigDecimal.ZERO;
  }

  public BigDecimal getPrice(BigDecimal priceActual, Opcrmopportunities opportunity,
      int stdPrecision, Product producto) {

    OBCriteria<QuotationOpportunity> quotationList = OBDal.getInstance()
        .createCriteria(QuotationOpportunity.class);
    quotationList
        .add(Restrictions.eq(QuotationOpportunity.PROPERTY_OPCRMOPPORTUNITIES, opportunity));
    if (quotationList.count() > 0) {
      OBCriteria<Promotion> promotion = OBDal.getInstance().createCriteria(Promotion.class);
      promotion.add(Restrictions.in(Promotion.PROPERTY_ECSFQQUOTATION, quotationList.list()));
      promotion.add(Restrictions.eq(Promotion.PROPERTY_ALERTSTATUS, true));

      for (Promotion promo : promotion.list()) {
        PriceAdjustment promocion = promo.getPromotionDiscount();
        if (promocion.getDiscountType().getId().equals("5D4BAF6BB86D4D2C9ED3D5A6FC051579")) {
          if (promo.getProduct().equals(producto)) {
            priceActual = priceActual.subtract(promo.getPromotionDiscount().getDiscountAmount())
                .multiply(BigDecimal.ONE.subtract(
                    promo.getPromotionDiscount().getDiscount().divide(BigDecimal.valueOf(100))))
                .setScale(stdPrecision, RoundingMode.HALF_UP);
          }
        }
      }
    }
    return priceActual;
  }

  public String getServletInfo() {
    return "This servlet manages manual transactions reconciliations.";
  }
    
  public boolean verifylocationpartner(String opportunitiesId) {
	  	Opcrmopportunities opportunity = OBDal.getInstance().get(Opcrmopportunities.class,
		        opportunitiesId);
	  	
	  	boolean verifylocation = false;
		try {
		  OBContext.setAdminMode(true);
		  OBCriteria<BusinessPartner> partner = OBDal.getInstance().createCriteria(BusinessPartner.class);
		  partner.add(Restrictions.eq(BusinessPartner.PROPERTY_ID, opportunity.getBusinessPartner().getId()));

		  if (partner.count() > 0) {
			  for (BusinessPartner partnerobj : partner.list()) {
				  if (partnerobj.getBusinessPartnerLocationList() != null && 
						    !partnerobj.getBusinessPartnerLocationList().isEmpty()) {
					  verifylocation =  true;
				  }
			  }
		  }		  
		} catch (Exception e) {
		  throw new OBException(e);
		} finally {
		  OBContext.restorePreviousMode();
		}
		return verifylocation;
  }
}
