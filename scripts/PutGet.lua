math.randomseed(os.time())

path = "/v0/entity"
ack=3
from=3
Repeats=true

threads=4
connections=4

method = "PUT"

local charset = {}
for i = 48,  57 do table.insert(charset, string.char(i)) end
for i = 65,  90 do table.insert(charset, string.char(i)) end
for i = 97, 122 do table.insert(charset, string.char(i)) end

function randomString(length)
  if length > 0 then
    return randomString(length - 1) .. charset[math.random(1, 62)]
  else
    return ""
  end
end

body=randomString(100)

counter=0

request = function()
counter=counter+1

if(counter%2==0) then
method = "PUT"
else
method = "GET"
end

if(Repeats==true) then
id="seq"..math.random(10)
else
id="seq"..counter
end

    return wrk.format(method, path.."?id="..id.."&replicas="..ack.."/"..from, nil, body)
end

done = function(summary, latency, requests)

file = io.open("LOADTEST.md", "a")

   file:write("------------------------------\n")
   if(Repeats==true) then
   file:write("PutGet ".."replicas="..ack.."/"..from.." with repeats\n")
   else 
   file:write("PutGet ".."replicas="..ack.."/"..from.." without repeats\n")
   end
   file:write("Threads: "..threads.." ; ".."Connections: "..connections.."\n")
   file:write("------------------------------\n")

   requests=summary["requests"]
   time=summary["duration"]
   file:write(string.format("Requests/sec: %g \n", requests/time*1000000))

   for _, p in pairs({ 90, 99}) do
      n = latency:percentile(p)
      file:write(string.format("Latency: %g%%,%g ms\n", p, n/1000))
   end

   min=latency.min
   file:write(string.format("min: %g ms\n", min/1000))

   max=latency.max 
   file:write(string.format("max: %g ms\n", max/1000))

   mean=latency.mean 
   file:write(string.format("mean: %g ms\n", mean/1000))

file:close()
end