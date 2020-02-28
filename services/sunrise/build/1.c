#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <dirent.h>
void mask_symbs(char * buf,int size){
    for (int i=0;i<size; i++){
     if (buf[i] == '.'){
         buf[i] = '_';
     }
    }
}

int main (){
    char buf1 [128];
    char buf2 [128];
    printf("Welcome to the Sunrise service. Enter command:");
    fflush(stdout);
    for (int i=0; i<5; i++){
	      gets(buf1);
        if (!strcmp(buf1,"put")){
          printf("Enter name:");fflush(stdout);
          gets(buf1);
          sprintf(buf2,"data/%s",buf1);
          FILE * f = fopen(buf2,"w+");
          if (!f){
              printf("Cannot write to file %s\n",buf2);
              return 0;
          }
          printf("Enter password:");fflush(stdout);
          gets(buf2);
          fputs(buf2,f);fputs("\n",f);
          printf("Enter receipt:");fflush(stdout);
          gets(buf2);
          fputs(buf2,f);fputs("\n",f);
          printf("Wrote\n");fflush(stdout);
          fclose(f);
        }
        else if (!strcmp(buf1,"get")){
          printf("Enter name:");fflush(stdout);
          gets(buf1);
          mask_symbs(buf1,strlen(buf1));
          sprintf(buf2,"data/%s",buf1);
          FILE * f = fopen(buf2,"r");
          if (!f){
            printf("Cannot read file %s\n",buf2);
            return 0;
          }
          fgets(buf1,64,f);
          char * real_path = strtok(buf1,"\n");
          strtok(buf2,"/");
          char * res = strtok(0,"/");
          printf(res);printf(":\n");fflush(stdout);
          printf("Enter password:");fflush(stdout);
          gets(buf2);
          if (!strcmp(buf2,real_path)){
            fgets(buf1,64,f);
            printf("Content:\n");fflush(stdout);
            printf(buf1);fflush(stdout);
          }
          else{
            printf(buf2);printf(" - invalid password\n");fflush(stdout);
          }
          fclose(f);
          fflush(stdout);
        }
        else if (!strcmp(buf1,"search")){
          printf("Enter pattern\n");fflush(stdout);
          gets(buf1);
          if (strlen(buf1) >=1){
            DIR *dir;
            struct dirent *ent;
            if ((dir = opendir ("data")) != NULL) {
              while ((ent = readdir (dir)) != NULL) {
                //strtok(ent->d_name,"/");
                //char * res = strtok(0,"/");
                if (strstr(ent->d_name,buf1)!=0){
                  printf ("%s\n", ent->d_name);fflush(stdout);
                }
              }
              closedir (dir);
            } else {
              printf("No such dir\n");fflush(stdout);
              return EXIT_FAILURE;
            }
          }
          else{
            printf("Too short pattern\n");fflush(stdout);
          }
        }
        else if (!strcmp(buf1,"rm")){
          printf("Enter name to del\n");fflush(stdout);
          gets(buf1);
          mask_symbs(buf1,strlen(buf1));
          sprintf(buf2,"data/%s",buf1);
          FILE * f = fopen(buf2,"r");
          if (!f){
            printf("Cannot read file %s\n",buf2);
            return 0;
          }
          fgets(buf1,64,f);
          fclose(f);
          char * real_pass = strtok(buf1,"\n");
          printf("Enter password:");fflush(stdout);
          char buf3[64];
          gets(buf3);
          if (!strcmp(buf3,real_pass)){
            sprintf(buf1,"rm %s",buf2);
            printf(buf1);
            system(buf1);
          }
        }
        else if (!strcmp(buf1,"help")){
          printf("put - store data\nget - load data\nsearch - find name\n");
          fflush(stdout);
        }
        else{
          printf("Invalid command\n");
          fflush(stdout);
          return 0;
        }
    }
}
