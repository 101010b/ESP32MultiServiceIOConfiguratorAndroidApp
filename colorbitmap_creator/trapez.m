function y = trapez(x, m, a, s)
if length(x) > 1
    y=x*0;
    for i=1:length(x)
        y(i) = trapez(x(i),m,a,s);
    end
    return;
end

x=(x-m)+0.5;
while x < 0
    x=x+1;
end
while x > 1
    x=x-1;
end
if x > 0.5
    x = 1-x;
end
x = 0.5-x;
if x < a/2
    y=1;
    return;
end
if x < a/2+s
    y = 1-(x - a/2)/s;
    return;
end
y = 0;
return;
    