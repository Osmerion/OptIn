import com.osmerion.optin.*;

import java.lang.annotation.*;

@RequiresOptIn
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Marker {}
