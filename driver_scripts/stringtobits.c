#include <stdio.h>
#include <stdlib.h>

void printbits(float f){
	int i = 0;
	char *c = &f;
	while(i < 4){
		printf("%c", c[i]);
		i++;
	}
}

int main (void) {
  	char buf[256];
  	while (fgets (buf, sizeof(buf), stdin)) {
   		float f = atof(buf);
    	printbits(f);
  	}
  	return 0;
}

