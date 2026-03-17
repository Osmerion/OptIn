import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@com.osmerion.optin.RequiresOptIn
public @interface MarkerWithWrongTargets {}
