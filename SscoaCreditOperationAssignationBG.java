package ec.com.sidesoft.credit.operation.assignation.ad_background;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.User;
import org.openbravo.scheduling.KillableProcess;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.db.DbUtility;

import ec.com.sidesoft.credit.factory.SscfCreditOperation;

public class SscoaCreditOperationAssignationBG extends DalBaseProcess implements KillableProcess {
	private final Logger log4j = Logger.getLogger(SscoaCreditOperationAssignationBG.class);
	private static ProcessLogger logger;
	private boolean killProcess = false;
	
	@Override
	public void kill(ProcessBundle processBundle) throws Exception {
		OBDal.getInstance().flush();
		this.killProcess = true;
	}

	@Override
	protected void doExecute(ProcessBundle bundle) throws Exception {
		// TODO Auto-generated method stub
		try {
			OBContext.setAdminMode(true);
			logger = bundle.getLogger();
			
			List<User> UvaluesList = getUsersDisponible();
	        if (UvaluesList.isEmpty()) {
	            logger.logln("No hay usuarios disponibles.");

	        }
			
			List<SscfCreditOperation> OCvaluesList = getOCDisponible();
	        if (OCvaluesList.isEmpty()) {
	            logger.logln("No hay operaciones disponibles.");

	        }
	        
	        // Creamos iteradores para ambas listas
	        Iterator<User> userIterator = UvaluesList.iterator();
	        Iterator<SscfCreditOperation> operationIterator = OCvaluesList.iterator();
			
	        //
	        
	        while (userIterator.hasNext() && operationIterator.hasNext()) {
	            User user = userIterator.next();  // Obtener siguiente usuario
	            SscfCreditOperation operation = operationIterator.next();  // Obtener siguiente operación

	            // Asignamos la operación al usuario
	            operation.setCallCenterUser(user);
	            operation.setGroundVerificationUser(user);
	            operation.setQualificationUser(user);

	            // Guardamos la operación con el usuario asignado
	            OBDal.getInstance().save(operation);  // Guardamos los cambios en la base de datos
	            
	            // Asignamos la operación al usuario
	            user.setSscoaAssigned(true);
	            // Guardamos el usuario asignado
	            OBDal.getInstance().save(user);  // Guardamos los cambios en la base de datos
	            
	            logger.logln("Usuario: " + user.getFirstName() + 
	            		"- Asignado a: "+ operation.getDocumentNo());
	        }
			
	        OBDal.getInstance().flush();
			
		}catch (Exception e) {
			
		    OBDal.getInstance().rollbackAndClose();
		    log4j.error("Exception found in Sbc_Reactivate: ", e);
			Throwable throwable = DbUtility.getUnderlyingSQLException(e);
			String message = OBMessageUtils.translateError(throwable.getMessage()).getMessage();
			logger.logln(e.getMessage());
			// TODO: handle exception
			e.printStackTrace();
		    throw new OBException(e.getMessage());
			
		}finally {
			OBContext.restorePreviousMode();
		}
	}

	public List<User> getUsersDisponible() throws Exception {
		ConnectionProvider conn = new DalConnectionProvider(false);
		PreparedStatement st = null;
		ResultSet rs = null;
		User Usu = null;
		String UserId = null;
		List<User> valuesList = new ArrayList<>();
		
		try {
			String sql = "WITH registros_por_dia AS ( \n"
					+ "    SELECT * \n"
					+ "    FROM sscoa_usersfactoryassign ufa \n"
					+ "    -- 1) Filtramos según el día de la semana \n"
					+ "    WHERE ( \n"
					+ "        (TRIM(TO_CHAR(NOW(),'Day')) = 'Monday'   AND ufa.monday   = 'Y') OR \n"
					+ "        (TRIM(TO_CHAR(NOW(),'Day')) = 'Tuesday'  AND ufa.tuesday  = 'Y') OR \n"
					+ "        (TRIM(TO_CHAR(NOW(),'Day')) = 'Wednesday'AND ufa.wednesday= 'Y') OR \n"
					+ "        (TRIM(TO_CHAR(NOW(),'Day')) = 'Thursday' AND ufa.thursday = 'Y') OR \n"
					+ "        (TRIM(TO_CHAR(NOW(),'Day')) = 'Friday'   AND ufa.friday   = 'Y') OR \n"
					+ "        (TRIM(TO_CHAR(NOW(),'Day')) = 'Saturday' AND ufa.saturday = 'Y') OR \n"
					+ "        (TRIM(TO_CHAR(NOW(),'Day')) = 'Sunday'   AND ufa.sunday   = 'Y') \n"
					+ "    ) \n"
					+ "), \n"
					+ "registros_filtrados AS ( \n"
					+ "    SELECT * \n"
					+ "    FROM registros_por_dia \n"
					+ "    -- 2) Sobre esos, solo los que ya han comenzado (ufadatefrom <= ahora) \n"
					+ "    WHERE ufadatefrom <= NOW() \n"
					+ "), \n"
					+ "registros_por_usuario AS ( \n"
					+ "    -- 3) Elegimos, para cada usuario, el registro más reciente \n"
					+ "    SELECT DISTINCT ON (ad_user_id) * \n"
					+ "    FROM registros_filtrados \n"
					+ "    ORDER BY ad_user_id, ufadatefrom DESC \n"
					+ ") \n"
					+ "SELECT \n"
					+ "    TO_CHAR(NOW(), 'Day')        AS nombre_dia, \n"
					+ "    ufa.ad_user_id, \n"
					+ "    ufa.ufadatefrom, \n"
					+ "    ufa.timein, \n"
					+ "    ufa.timeout, \n"
					+ "    ufa.timestartlunch, \n"
					+ "    ufa.timeendlunch, \n"
					+ "    ufa.isactive, \n"
					+ "    ufa.monday, \n"
					+ "    ufa.tuesday, \n"
					+ "    ufa.wednesday, \n"
					+ "    ufa.thursday, \n"
					+ "    ufa.friday, \n"
					+ "    ufa.saturday, \n"
					+ "    ufa.sunday \n"
					+ "FROM registros_por_usuario ufa \n"
					+ "JOIN ad_user au \n"
					+ "  ON au.ad_user_id = ufa.ad_user_id \n"
					+ "-- 4) Aplicamos el resto de filtros \n"
					+ "WHERE au.isactive          = 'Y' \n"
					+ "  AND ufa.isactive         = 'Y' \n"
					+ "  AND au.em_sscoa_assigned = 'N' \n"
					+ "  AND CURRENT_TIME BETWEEN ufa.timein::time AND ufa.timeout::time \n"
					+ "  AND ( \n"
					+ "        ufa.timestartlunch IS NULL \n"
					+ "     OR ufa.timeendlunch   IS NULL \n"
					+ "     OR NOT (CURRENT_TIME BETWEEN ufa.timestartlunch::time AND ufa.timeendlunch::time) \n"
					+ "      ) \n"
					+ "ORDER BY ufa.ad_user_id;";
			st = conn.getPreparedStatement(sql);
//			st.setString(1, identifier);
			rs = st.executeQuery();
			while (rs.next()) {
				UserId = rs.getString("ad_user_id");
				Usu = OBDal.getInstance().get(User.class, UserId);
				valuesList.add(Usu);
				
			}

		} catch (Exception e) {
			e.printStackTrace();
			String msg = "Error: " + e.getMessage();
		} finally {

			if (rs != null) {
				rs.close();
			}

			if (st != null) {
				st.close();
			}

			// No cierres la conexión aquí, ya que la necesitas para la consulta a
			// continuación.

		}
		
		return valuesList;
	}
	
	public List<SscfCreditOperation> getOCDisponible() throws Exception {
		ConnectionProvider conn = new DalConnectionProvider(false);
		PreparedStatement st = null;
		ResultSet rs = null;
		SscfCreditOperation CreditO = null;
		String CreditOId = null;
		List<SscfCreditOperation> valuesList = new ArrayList<>();
		
		try {
			String sql = "select sscf_credit_operation_id \n"
					+ "from sscf_credit_operation \n"
					+ "where Com2_Complete is not null \n"
					+ "AND Ground_Verification_User_ID is null \n"
					+ "AND Qualification_User_ID is null \n"
					+ "AND Call_Center_User_ID is null \n"
					+ "order by Com2_Complete;";
			st = conn.getPreparedStatement(sql);
//			st.setString(1, identifier);
			rs = st.executeQuery();
			while (rs.next()) {
				CreditOId = rs.getString("sscf_credit_operation_id");
				CreditO = OBDal.getInstance().get(SscfCreditOperation.class, CreditOId);
				valuesList.add(CreditO);
				
			}

		} catch (Exception e) {
			e.printStackTrace();
			String msg = "Error: " + e.getMessage();
		} finally {

			if (rs != null) {
				rs.close();
			}

			if (st != null) {
				st.close();
			}

			// No cierres la conexión aquí, ya que la necesitas para la consulta a
			// continuación.

		}
		
		return valuesList;
	}

}
