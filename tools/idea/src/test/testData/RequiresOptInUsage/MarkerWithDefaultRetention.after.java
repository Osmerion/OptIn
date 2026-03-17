import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@com.osmerion.optin.RequiresOptIn
public @interface MarkerWithDefaultRetention {}
