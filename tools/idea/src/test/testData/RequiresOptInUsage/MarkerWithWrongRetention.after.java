import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@com.osmerion.optin.RequiresOptIn
@Retention(RetentionPolicy.RUNTIME)
public @interface MarkerWithWrongRetention {}
