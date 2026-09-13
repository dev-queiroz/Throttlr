local current_key = KEYS[1]
local previous_key = KEYS[2]

local limit = tonumber(ARGV[1])
local window_ms = tonumber(ARGV[2])
local cost = tonumber(ARGV[3])
local now_ms = tonumber(ARGV[4])

local current_window = math.floor(now_ms / window_ms)
local window_start = current_window * window_ms
local elapsed = now_ms - window_start
local previous_weight = (window_ms - elapsed) / window_ms

local current = tonumber(redis.call('GET', current_key))
if current == nil then
  current = 0
end

local previous = tonumber(redis.call('GET', previous_key))
if previous == nil then
  previous = 0
end

local estimated = current + (previous * previous_weight)
local allowed = 0
local remaining = math.max(0, math.floor(limit - estimated))
local retry_after_ms = window_ms - elapsed

if estimated + cost <= limit then
  allowed = 1
  current = redis.call('INCRBY', current_key, cost)
  redis.call('PEXPIRE', current_key, window_ms * 2)
  remaining = math.max(0, math.floor(limit - (current + (previous * previous_weight))))
  retry_after_ms = 0
end

return { allowed, remaining, limit, retry_after_ms }
