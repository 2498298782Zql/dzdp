---秒杀券id
local voucherId = ARGV[1]
--用户id
local userId = ARGV[2]
--订单id
local id = ARGV[3]

--库存key
local stockKey = 'seckill:stock:' .. voucherId
--订单key
local orderKey = 'seckill:order:' .. voucherId

--库存是否充足
--库存不足
if (tonumber(redis.call('get', stockKey)) <= 0) then
    return 1
end

--判断用户是否下单
--存在用户 禁止重复下单
if (tonumber(redis.call('sismember', orderKey, userId)) == 1) then
    return 2
end

--扣减库存
redis.call('incrby',stockKey,-1)
--下单（保存用户）
redis.call('sadd',orderKey,userId)
--发送消息
redis.call('xadd','stream.orders','*','userId',userId,'voucherId',voucherId,'id',id)
return 0

