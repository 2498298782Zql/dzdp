package com.dzdp.mq;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dzdp.entity.VoucherOrder;
import com.dzdp.mapper.VoucherOrderMapper;
import com.dzdp.service.ISeckillVoucherService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
public class SecKillConsumer extends ServiceImpl<VoucherOrderMapper,VoucherOrder> {

    @Resource
    ISeckillVoucherService seckillVoucherService;

    @RocketMQMessageListener(topic = "secKill", selectorExpression = "voucherOrder", consumerGroup = "secKillConsumer")
    public class ConsumerSend implements RocketMQListener<VoucherOrder>{
        @Override
        public void onMessage(VoucherOrder voucherOrder) {

            boolean success = seckillVoucherService.update()
                    .setSql("stock = stock - 1") // set stock = stock - 1
                    .eq("voucher_id", voucherOrder.getVoucherId()).gt("stock", 0) // where id = ? and stock > 0
                    .update();
            if(success){
                log.info("消费订单:"+voucherOrder.getId());
                save(voucherOrder);
            }else{
                log.info(Thread.currentThread().getName()+"消费失败，订单id为:"+voucherOrder.getId());
                throw new RuntimeException();
            }


        }
    }


}
