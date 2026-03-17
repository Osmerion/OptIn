import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target({ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.METHOD, ElementType.MODULE, ElementType.PACKAGE, ElementType.TYPE})
@com.osmerion.optin.RequiresOptIn
public @interface MarkerWithDefaultTargets {}
