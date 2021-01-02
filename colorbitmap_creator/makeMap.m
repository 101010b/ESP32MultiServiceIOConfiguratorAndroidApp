X = (0:511)/511;
Y = X';

W = Y*0;
C = Y*0;
for i=1:length(X)
    x = X(i);
    if x < 0.5
        C(i) = 2*x;
        W(i) = 0;
    else
        C(i) = 2-2*x;
        W(i) = 2*(x-0.5);
    end
end
W=flipud(W);
C=flipud(C);

R = zeros(512,512);
G = zeros(512,512);
B = zeros(512,512);

r = trapez(X, 0, 1/3, 1/6);
g = trapez(X, 1/3, 1/3, 1/6);
b = trapez(X, 2/3, 1/3, 1/6);

for i=1:length(X)
    R(1:512,i) = C*r(i) + W;
    G(1:512,i) = C*g(i) + W;
    B(1:512,i) = C*b(i) + W;
end
R(R > 1.0) = 1.0;
G(G > 1.0) = 1.0;
B(B > 1.0) = 1.0;

im = cat(3, R, G, B);

image(im);
imwrite(im, 'colormap.png');