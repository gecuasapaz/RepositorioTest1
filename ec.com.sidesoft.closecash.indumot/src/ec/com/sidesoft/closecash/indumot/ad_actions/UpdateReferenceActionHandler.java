package ec.com.sidesoft.closecash.indumot.ad_actions;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.service.db.DalConnectionProvider;

import ec.com.sidesoft.custom.closecash.ScccCashClousure;

public class UpdateReferenceActionHandler extends BaseActionHandler {

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String data) {
    JSONObject response = new JSONObject();

    try {
      OBContext.setAdminMode(true);
      final JSONObject jsonData = new JSONObject(data);
      final String id = jsonData.getString("id");

      // Obtenemos la oportunidad
      FIN_FinaccTransaction transaction = OBDal.getInstance().get(FIN_FinaccTransaction.class, id);

      FIN_Payment payment = transaction.getFinPayment();
      if (payment != null) {
        payment.setReferenceNo(transaction.getSscccinReference());

        OBDal.getInstance().save(payment);
        OBDal.getInstance().flush();
      }

      ScccCashClousure cashClosure = transaction.getScccCashClousure();
      if (cashClosure != null) {
        updateReference(cashClosure.getId(), transaction.getSscccinReference());
      }

      response.put("status", "OK");
    } catch (Exception e) {
      System.out.println("UpdateReferenceActionHandler: " + e.getMessage());
      try {
        response.put("status", "ERROR");
        response.put("message", e.getMessage());
      } catch (Exception e2) {
      }
    } finally {
        OBContext.restorePreviousMode();
    }
    return response;
  }

  private void updateReference(String cashClosureId, String reference) {
    ConnectionProvider conn = new DalConnectionProvider(false);

    try {
      String sql = "SELECT sscccin_update_reference(?,?) AS result";
      PreparedStatement st = conn.getPreparedStatement(sql);
      st.setString(1, cashClosureId);
      st.setString(2, reference);
      ResultSet rs = st.executeQuery();

      while (rs.next()) {
        System.out.println(rs.getString("result"));
      }
    } catch (Exception e) {
      throw new OBException("getAttributes: " + e.getMessage());
    }
  }
}
