package ec.com.sidesoft.closecash.indumot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.openbravo.client.kernel.BaseComponentProvider;
import org.openbravo.client.kernel.Component;
import org.openbravo.client.kernel.ComponentProvider;

@ApplicationScoped
@ComponentProvider.Qualifier(SscccinComponentProvider.QUALIFIER)
public class SscccinComponentProvider extends BaseComponentProvider {
  public static final String QUALIFIER = "SSCCCIN_ComponentProvider";

  @Override
  public Component getComponent(String componentId, Map<String, Object> parameters) {
    throw new IllegalArgumentException("Component id " + componentId + " not supported."); 
  }

  @Override
  public List<ComponentResource> getGlobalComponentResources() {

    final List<ComponentResource> globalResources = new ArrayList<ComponentResource>();
    final String prefix = "web/ec.com.sidesoft.closecash.indumot";

    globalResources.add(createStaticResource(prefix + "/js/updateReference.js", false));

    return globalResources;
  }

  //Metodo
	public int dividir(int a, int b) {
        a = a + 1;
        return a / b;
    }
  }

  public int calculoProvider(int a, int c){
        int sum = a+ c;
        return sum; 
  }

}