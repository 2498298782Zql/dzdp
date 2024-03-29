import com.dzdp.DZDianPingApplication;
import com.dzdp.dto.Result;
import com.dzdp.service.IUserService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;


@Slf4j
@SpringBootTest(classes = DZDianPingApplication.class)
public class UserServiceTest {

    @Resource
    IUserService userService;


    @Test
    public void sendCodeTest(){
        String email = "12345";
        Result result = userService.sendMailCode(email);
        System.out.println(result);
        email = "zql249829@163.com";
        result = userService.sendMailCode(email);
        System.out.println(result);
        result = userService.sendMailCode(email);
        System.out.println(result);
        result = userService.sendMailCode(email);
        System.out.println(result);
        result = userService.sendMailCode(email);
        System.out.println(result);
    }


}
