math.randomseed(os.time())

--меняем переменные
method = "GET"
path = "/v0/entity"
ack=3
from=3
Repeats=true--получение/запись с повторами или без

--просто для вывода
threads=4
connections=4
--дальше не меняем

--последовательные id через счетчик (без повторов)
counter=0

request = function()
if(Repeats==true) then
id="seq"..math.random(10)--id с частыми повторами
else
counter=counter+1
id="seq"..counter
end

    return wrk.format(method, path.."?id="..id.."&replicas="..ack.."/"..from, nil, nil)
end

--вывод в файл
done = function(summary, latency, requests)
-- Открытие файла с режимом добавления
file = io.open("LOADTEST.md", "a")

-- разделительная строка между тестами
   file:write("------------------------------\n")
   if(Repeats==true) then
   file:write(method.."replicas="..ack.."/"..from.." with repeats\n")
   else 
   file:write(method.."replicas="..ack.."/"..from.." without repeats\n")
   end
   file:write("Threads: "..threads.." ; ".."Connections: "..connections.."\n")
   file:write("------------------------------\n")
  
  --минимальное значение задержки
   requests=summary["requests"]
   time=summary["duration"]
   file:write(string.format("Requests/sec: %g \n", requests/time*1000000))

  --деление 1000 - ms; 1- ns 
  --перцентили
   for _, p in pairs({ 90, 99}) do
      n = latency:percentile(p)
      file:write(string.format("Latency: %g%%,%g ms\n", p, n/1000))
   end

   --минимальное значение задержки
   min=latency.min
   file:write(string.format("min: %g ms\n", min/1000))

   --максимальное значение задержки
   max=latency.max 
   file:write(string.format("max: %g ms\n", max/1000))

   --среднее значение задержки
   mean=latency.mean 
   file:write(string.format("mean: %g ms\n", mean/1000))


   -- закрываем файл
file:close()
end
