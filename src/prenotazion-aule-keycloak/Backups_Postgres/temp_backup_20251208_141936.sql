docker : pg_dumpall: error: connection to server on socket "/var/run/postgresql/.s.PGSQL.5432" failed: FATAL:  role 
"postgres" does not exist
In C:\Users\Nunzio\OneDrive\Desktop\progetto_ss\prenotazion-aule-keycloak\prenotazion-aule-keycloak\backup_db.ps1:22 
car:5
+     docker exec -i $ContainerName pg_dumpall -c -U $DbUser > $SqlFile ...
+     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    + CategoryInfo          : NotSpecified: (pg_dumpall: err... does not exist:String) [], RemoteException
    + FullyQualifiedErrorId : NativeCommandError
 
