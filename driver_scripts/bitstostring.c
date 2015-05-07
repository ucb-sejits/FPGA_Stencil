#include <stdio.h>
#include <stdlib.h>


int main (void) {
  	char buf[256];
  	int i = 0;
  	float *f;
  	while (fgets (buf, sizeof(buf), stdin)) {
  		printf("%s\n", buf);
  		while (i < 32) {
  			if (! (i%4)) {
  				f = &buf[i];
  				printf("%f\n", *f);
  			}
  			i++;
  		}
 	}
  	return 0;
}