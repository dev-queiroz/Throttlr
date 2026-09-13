local tokens_key = KEYS[1]
local timestamp_key = KEYS[2]

local capacity = tonumber(ARGV[1])
local refill_rate = tonumber(ARGV[2])
local cost = tonumber(ARGV[3])
local now_ms = tonumber(ARGV[4])
local ttl_ms = tonumber(ARGV[5])

local current_tokens = tonumber(redis.call('GET', tokens_key))
if current_tokens == nil then
  current_tokens = capacity
end

local last_refill_ms = tonumber(redis.call('GET', timestamp_key))
if last_refill_ms == nil then
  last_refill_ms = now_ms
end

local elapsed_ms = math.max(0, now_ms - last_refill_ms)
local refill = (elapsed_ms / 1000.0) * refill_rate
local available = math.min(capacity, current_tokens + refill)

local allowed = 0
local retry_after_ms = 0

if available >= cost then
  allowed = 1
  available = available - cost
else
  retry_after_ms = math.ceil(((cost - available) / refill_rate) * 1000)
end

redis.call('SET', tokens_key, available, 'PX', ttl_ms)
redis.call('SET', timestamp_key, now_ms, 'PX', ttl_ms)

return { allowed, math.floor(available), capacity, retry_after_ms }
