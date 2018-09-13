local key=KEYS[1];
local ttl=ARGV[1];
local ticket=ARGV[2];

local flag=redis.call('setnx', key,ticket);
if(flag) then
    redis.call("expire",key,ttl);
end
return flag