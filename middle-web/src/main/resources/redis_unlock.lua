local key=KEYS[1];
local ticket=ARGV[1];

local val=redis.call('get', key);
if(val==ticket) then
    redis.call("del",key);
end