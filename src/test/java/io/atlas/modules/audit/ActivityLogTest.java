package io.atlas.modules.audit;
import io.atlas.modules.audit.service.ActivityLogService;
public final class ActivityLogTest {
 public static void main(String[] args) {
  for(String value:new String[]{"/recuperarsenha confirmar 12345678 private-secret private-secret","/email cadastrar private-secret@example.invalid","/email confirmar 12345678","/login private-secret","/register private-secret private-secret","/site vincular 123456","senha=private-secret","https://discord.com/api/webhooks/123456/private-secret"}) {
   String clean=ActivityLogService.redact(value);
   if(clean.contains("private-secret")||clean.contains("123456"))throw new AssertionError("Credencial não removida");
  }
  if(!ActivityLogService.redact("Olá, preciso de ajuda!").equals("Olá, preciso de ajuda!"))throw new AssertionError("Mensagem normal alterada");
  System.out.println("ActivityLogTest PASS: sensitive values redacted; ordinary chat preserved");
 }
}
