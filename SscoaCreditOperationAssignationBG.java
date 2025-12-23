package ec.com.sidesoft.credit.operation.assignation.ad_background;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
	            
	            // Registrar en bitácora la asignación
	            registerAssignationInBinnacle(operation, user);
	            
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
			String sql = "WITH dia_actual AS ( \n"
					+ "    SELECT EXTRACT(ISODOW FROM NOW())::int AS hoy \n"
					+ "), \n"
					+ "registros_activos AS ( \n"
					+ "    SELECT ufa.* \n"
					+ "    FROM sscoa_usersfactoryassign ufa \n"
					+ "    CROSS JOIN dia_actual d \n"
					+ "    WHERE ufa.isactive = 'Y' \n"
					+ "      AND ( \n"
					+ "        (d.hoy = 1 AND ufa.monday='Y') OR \n"
					+ "        (d.hoy = 2 AND ufa.tuesday='Y') OR \n"
					+ "        (d.hoy = 3 AND ufa.wednesday='Y') OR \n"
					+ "        (d.hoy = 4 AND ufa.thursday='Y') OR \n"
					+ "        (d.hoy = 5 AND ufa.friday='Y') OR \n"
					+ "        (d.hoy = 6 AND ufa.saturday='Y') OR \n"
					+ "        (d.hoy = 7 AND ufa.sunday='Y') \n"
					+ "      ) \n"
					+ "), \n"
					+ "registros_filtrados AS ( \n"
					+ "    SELECT DISTINCT ON (r.ad_user_id) r.* \n"
					+ "    FROM registros_activos r \n"
					+ "    WHERE r.ufadatefrom <= NOW() \n"
					+ "    ORDER BY r.ad_user_id, r.ufadatefrom DESC \n"
					+ "), \n"
					+ "			ultima_asignacion AS ( \n"
					+ "    SELECT  \n"
					+ "        ufa.ad_user_id,  \n"
					+ "        COALESCE(liberacion_real.hora_liberacion, '1900-01-01'::timestamp) AS ultima_asignacion  \n"
					+ "    FROM registros_filtrados ufa  \n"
					+ "    LEFT JOIN ( \n"
					+ "        -- Usar window functions para obtener la siguiente secuencia (MÁS RÁPIDO) \n"
					+ "        SELECT DISTINCT ON (sb1.ad_user_id) \n"
					+ "               sb1.ad_user_id, \n"
					+ "               sb1.sscf_credit_operation_id, \n"
					+ "               sb1.created AS ultima_fecha_creacion, \n"
					+ "               sb1.time_assign AS hora_liberacion \n"
					+ "        FROM sscf_binnacle sb1 \n"
					+ "        WHERE sb1.ad_user_id IS NOT NULL \n"
					+ "          AND sb1.time_assign IS NOT NULL \n"
					+ "        ORDER BY sb1.ad_user_id, sb1.time_assign DESC \n"
					+ "    ) liberacion_real ON liberacion_real.ad_user_id = ufa.ad_user_id \n"
					+ ") \n"
					+ "SELECT  \n"
					+ "    TO_CHAR(NOW(), 'Day')        AS nombre_dia,  \n"
					+ "    ufa.ad_user_id,  \n"
					+ "    ufa.ufadatefrom,  \n"
					+ "    ufa.timein,  \n"
					+ "    ufa.timeout,  \n"
					+ "    ufa.timestartlunch,  \n"
					+ "    ufa.timeendlunch,  \n"
					+ "    ufa.isactive,  \n"
					+ "    ufa.monday,  \n"
					+ "    ufa.tuesday,  \n"
					+ "    ufa.wednesday,  \n"
					+ "    ufa.thursday,  \n"
					+ "    ufa.friday,  \n"
					+ "    ufa.saturday,  \n"
					+ "    ufa.sunday,  \n"
					+ "    ua.ultima_asignacion  \n"
					+ "FROM registros_filtrados ufa  \n"
					+ "JOIN ad_user au  \n"
					+ "  ON au.ad_user_id = ufa.ad_user_id  \n"
					+ "JOIN ultima_asignacion ua  \n"
					+ "  ON ua.ad_user_id = au.ad_user_id  \n"
					+ "WHERE au.isactive          = 'Y'  \n"
					+ "  AND ufa.isactive         = 'Y'  \n"
					+ "  AND au.em_sscoa_assigned = 'N'  \n"
					+ "  AND CURRENT_TIME BETWEEN ufa.timein::time AND ufa.timeout::time  \n"
					+ "  AND (  \n"
					+ "        ufa.timestartlunch IS NULL  \n"
					+ "     OR ufa.timeendlunch   IS NULL  \n"
					+ "     OR NOT (CURRENT_TIME BETWEEN ufa.timestartlunch::time AND ufa.timeendlunch::time) \n" 
					+ "      )  \n"
					+ "ORDER BY ua.ultima_asignacion ASC, ufa.ad_user_id;";
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
					+ "AND Qualification_User_ID is null \n"
					+ "AND Call_Center_User_ID is null \n"
					+ "AND docstatus NOT IN ('R','PW') \n" //Nueva validacion de estados Operaciones Ticket 20161
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
	
	/**
	 * Actualiza en la bitácora la asignación de un usuario a una operación de crédito
	 * Busca la línea existente COM-D → FCR-C y la actualiza con la información del usuario asignado
	 * @param operation Operación de crédito asignada
	 * @param user Usuario asignado
	 */
	private void registerAssignationInBinnacle(SscfCreditOperation operation, User user) {
		ConnectionProvider conn = new DalConnectionProvider(false);
		
		try {
			// Asignamos la operación al usuario
            operation.setCallCenterUser(user);
            operation.setQualificationUser(user);

            // Guardamos la operación con el usuario asignado
            OBDal.getInstance().save(operation);  // Guardamos los cambios en la base de datos
            
            // Asignamos la operación al usuario
            user.setSscoaAssigned(true);
            // Guardamos el usuario asignado
            OBDal.getInstance().save(user);  // Guardamos los cambios en la base de datos
			
			// Obtener horarios del usuario desde sscoa_usersfactoryassign
			String horariosSql = "SELECT timein, timestartlunch, timeendlunch, timeout " +
						"FROM sscoa_usersfactoryassign " +
						"WHERE ad_user_id = ? AND isactive = 'Y' " +
						"AND ufadatefrom <= NOW() " +
						"ORDER BY ufadatefrom DESC LIMIT 1";
			
			Timestamp timeStart = null;
			Timestamp timeLunchIn = null;
			Timestamp timeLunchOut = null;
			Timestamp timeEnd = null;
			
			try (PreparedStatement horariosSt = conn.getPreparedStatement(horariosSql)) {
				horariosSt.setString(1, user.getId());
				try (ResultSet horariosRs = horariosSt.executeQuery()) {
					if (horariosRs.next()) {
						// Leer como Timestamp y extraer solo la hora
						Timestamp timeInTs = horariosRs.getTimestamp("timein");
						Timestamp timeStartLunchTs = horariosRs.getTimestamp("timestartlunch");
						Timestamp timeEndLunchTs = horariosRs.getTimestamp("timeendlunch");
						Timestamp timeOutTs = horariosRs.getTimestamp("timeout");
						
						// Obtener fecha actual
						java.time.LocalDate today = java.time.LocalDate.now();
						
						// Convertir a LocalDateTime combinando fecha actual con hora del horario
						if (timeInTs != null) {
							java.time.LocalTime timeIn = timeInTs.toLocalDateTime().toLocalTime();
							timeStart = Timestamp.valueOf(today.atTime(timeIn));
						}
						if (timeStartLunchTs != null) {
							java.time.LocalTime timeStartLunch = timeStartLunchTs.toLocalDateTime().toLocalTime();
							timeLunchIn = Timestamp.valueOf(today.atTime(timeStartLunch));
						}
						if (timeEndLunchTs != null) {
							java.time.LocalTime timeEndLunch = timeEndLunchTs.toLocalDateTime().toLocalTime();
							timeLunchOut = Timestamp.valueOf(today.atTime(timeEndLunch));
						}
						if (timeOutTs != null) {
							java.time.LocalTime timeOut = timeOutTs.toLocalDateTime().toLocalTime();
							timeEnd = Timestamp.valueOf(today.atTime(timeOut));
						}
					}
				}
			}
			
			// Actualizar la línea existente COM-D → FCR-C con la información del usuario asignado
			String updateSql = "UPDATE sscf_binnacle SET " +
					"ad_user_id = ?, " +
					"time_assign = ?, " +
					"time_start = ?, " +
					"time_lunch_in = ?, " +
					"time_lunch_out = ?, " +
					"time_end = ?, " +
					"updated = ?, " +
					"updatedby = ? " +
					"WHERE sscf_credit_operation_id = ? " +
					"AND artboard_from = 'COM2' " +
					"AND artboard_to = 'CC' " +
					"AND line = (SELECT MAX(line) FROM sscf_binnacle WHERE sscf_credit_operation_id = ? AND artboard_from = 'COM2' AND artboard_to = 'CC')";
			
			try (PreparedStatement updateSt = conn.getPreparedStatement(updateSql)) {
				updateSt.setString(1, user.getId()); // ad_user_id
				updateSt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now())); // time_assign
				updateSt.setTimestamp(3, timeStart); // time_start
				updateSt.setTimestamp(4, timeLunchIn); // time_lunch_in
				updateSt.setTimestamp(5, timeLunchOut); // time_lunch_out
				updateSt.setTimestamp(6, timeEnd); // time_end
				updateSt.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now())); // updated
				updateSt.setString(8, OBContext.getOBContext().getUser().getId()); // updatedby
				updateSt.setString(9, operation.getId()); // sscf_credit_operation_id
				updateSt.setString(10, operation.getId()); // sscf_credit_operation_id
				
				int rowsAffected = updateSt.executeUpdate();
				if (rowsAffected > 0) {
					log4j.info("Actualización en bitácora exitosa para operación: " + operation.getDocumentNo() + 
							" - Usuario asignado: " + user.getName());
				} else {
					log4j.warn("No se encontró línea COM-D → FCR-C para actualizar en operación: " + operation.getDocumentNo());
				}
			}
			
		} catch (Exception e) {
			log4j.error("Error al actualizar asignación en bitácora: ", e);
		}
	}

}
