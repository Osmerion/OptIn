import com.osmerion.optin.*;
import org.orekit.annotation.*;
import org.orekit.time.*;

public class HelloWorld {

    @OptIn(org.orekit.annotation.DefaultDataContext.class)
    public void foo() {
        new AbsoluteDate();
    }

}
