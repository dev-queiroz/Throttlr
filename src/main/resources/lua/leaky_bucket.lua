local level_key = KEYS[1]
local timestamp_key = KEYS[2]

local capacity = tonumber(ARGV[1])
local leak_rate = tonumber(ARGV[2])
local cost = tonumber(ARGV[3])
local now_ms = tonumber(ARGV[4])
local ttl_ms = tonumber(ARGV[5])

local level = tonumber(redis.call('GET', level_key))
if level == nil then
  level = 0
end

local last_seen_ms = tonumber(redis.call('GET', timestamp_key))
if last_seen_ms == nil then
  last_seen_ms = now_ms
end

local elapsed_ms = math.max(0, now_ms - last_seen_ms)
local leaked = (elapsed_ms / 1000.0) * leak_rate
level = math.max(0, level - leaked)

local allowed = 0
local retry_after_ms = 0

if level + cost <= capacity then
  allowed = 1
  level = level + cost
else
  retry_after_ms = math.ceil(((level + cost - capacity) / leak_rate) * 1000)
end

redis.call('SET', level_key, level, 'PX', ttl_ms)
redis.call('SET', timestamp_key, now_ms, 'PX', ttl_ms)

local remaining = math.max(0, math.floor(capacity - level))
return { allowed, remaining, capacity, retry_after_ms }
