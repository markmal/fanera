# fanera
This program loads an STL file (for example a model of wing exported from XFLR5) and fills it with plywood texture.
So it looks like the model is carved out of plywood.
Then it can be printed in natural sizes on (wax)paper or film to be used when real plywood will be manually processed 
to make real part (i.e. wing).
Idea is to follow plywood wood-glue pattern created by program, that should result in shape of the real part will be 
very close to 3D profile of the designed part.

Features:
- Perspective projection for viewing and parallel projection for printing to keep precise real sizes;
- Fast buttons for ortogonal views: face, back and side
- Adjustment of parameters of virtual plywood: ply thickness, glue layer thickness ratio;
- Multi-page printing with manual adjustment to pages selection;
- 1 cm grid on virtual part;
- Export to PNG image with real sizes embedded into file (pixels per meter);
  that should allow to print your drawings at commercial printing services on big film if you do not have such printer;

The program is written in Java and works in any OS that supports Java 8 SE.
Requires:
- Java SE v8 or later, JRE
- Java 3D™ 1.5.1 API (by Oracle, free)
- Java™ Advanced Imaging API v1.1.2_01 or later (by Oracle, free)
- Open j3d libraries (open source, free)

For inforation: Fanera translates from Russian (Фанера) as Plywood.
