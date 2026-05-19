package com.ifsp.Auroraprojeto;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;

@Controller
public class AuroraController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ConteudoRepository conteudoRepository;

    @Autowired
    private CalendarioRepository calendarioRepository;

    // CREDENCIAIS DO ADMIN ÚNICO
    private final String ADMIN_USER = "admin_aurora";
    private final String ADMIN_PASS = "aurora123";

    // ================= LOGIN / LOGOUT GERAL =================

    @GetMapping("/login")
    public String telaLogin(HttpSession session) {
        if (usuarioLogado(session)) return "redirect:/inicio";
        return "Login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email, @RequestParam String senha, HttpSession session) {
        Usuario usuario = usuarioService.login(email, senha);
        if (usuario != null) {
            session.setAttribute("usuario", usuario);
            return "redirect:/inicio";
        }
        return "redirect:/login?erro=true";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate(); 
        return "redirect:/login";
    }

    // ================= CADASTRO DE ALUNO =================

    @GetMapping("/cadastro")
    public String telaCadastro(HttpSession session, Model model) {
        if (usuarioLogado(session)) return "redirect:/inicio";
        model.addAttribute("usuario", new Usuario());
        return "Cadastro"; 
    }

    @PostMapping("/cadastro")
    public String realizarCadastro(@ModelAttribute Usuario usuario) {
        boolean sucesso = usuarioService.cadastrar(usuario);

        if (sucesso) {
            return "redirect:/login?sucesso=true";
        } else {
            return "redirect:/cadastro?erro=email_existente";
        }
    }

    // ================= ÁREA DO ALUNO =================

    @GetMapping("/inicio")
    public String inicio(HttpSession session, Model model) {
        if (!usuarioLogado(session)) return "redirect:/login";
        model.addAttribute("usuario", (Usuario) session.getAttribute("usuario"));

        Usuario usuario = (Usuario) session.getAttribute("usuario");

        if (usuario != null) {
            model.addAttribute("cidade", usuario.getCidade());
        }
     
        List<Calendario> listaEventos = calendarioRepository.findAll();
        model.addAttribute("eventos", listaEventos);

        return "TelaInicio";
    }

    @GetMapping("/perfil")
    public String perfil(HttpSession session, Model model) {
        if (!usuarioLogado(session)) return "redirect:/login";
        model.addAttribute("usuario", (Usuario) session.getAttribute("usuario"));
        return "PerfilAluno";
    }

    @PostMapping("/salvarPerfil")
    public String salvarPerfil(@ModelAttribute Usuario usuarioAtualizado, HttpSession session) {
        if (!usuarioLogado(session)) return "redirect:/login";
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        usuario.setNome(usuarioAtualizado.getNome());
        usuario.setEmail(usuarioAtualizado.getEmail());
        usuario.setCurso(usuarioAtualizado.getCurso());
        usuario.setCidade(usuarioAtualizado.getCidade());
        usuario.setTelefone(usuarioAtualizado.getTelefone());
        usuarioService.salvar(usuario);
        session.setAttribute("usuario", usuario);
        return "redirect:/perfil";
    }

    @GetMapping("/disciplinas")
    public String disciplinas(HttpSession session, Model model) {
        if (!usuarioLogado(session)) return "redirect:/login";
        model.addAttribute("usuario", (Usuario) session.getAttribute("usuario"));
        return "TelaDisciplinas";
    }

    // NOVA ROTA DE EXERCÍCIOS: Totalmente dinâmica e blindada contra erro 404
    @GetMapping("/exercicios")
    public String exercicios(
            @RequestParam(value = "disc", required = false) String disc,
            @RequestParam(value = "nivel", required = false) String nivel,
            HttpSession session, Model model) {
        
        if (!usuarioLogado(session)) return "redirect:/login";
        
        List<Conteudo> listaExercicios;

        // Se o aluno escolheu uma disciplina e nível específicos (ex: Matemática Básico)
        if (disc != null && !disc.isEmpty() && nivel != null && !nivel.isEmpty()) {
            try {
                Disciplina disciplinaEnum = Disciplina.valueOf(disc.toUpperCase());
                List<Conteudo> doBanco = conteudoRepository.findByDisciplinaAndNivel(disciplinaEnum, nivel);
                
                if (doBanco != null) {
                    listaExercicios = doBanco.stream()
                        .filter(c -> TipoConteudo.EXERCICIO.equals(c.getTipo()))
                        .toList();
                } else {
                    listaExercicios = new ArrayList<>();
                }
            } catch (IllegalArgumentException e) {
                listaExercicios = new ArrayList<>();
            }
            model.addAttribute("disciplinaAtiva", disc);
            model.addAttribute("nivelAtivo", nivel);
        } else {
            // Se o aluno entrou direto sem filtros, traz todas as listas do tipo EXERCICIO
            List<Conteudo> todosExercicios = conteudoRepository.findByTipo(TipoConteudo.EXERCICIO);
            listaExercicios = todosExercicios != null ? todosExercicios : new ArrayList<>();
        }

        model.addAttribute("exercicios", listaExercicios);
        
        List<Calendario> eventos = calendarioRepository.findAll();
        model.addAttribute("eventos", eventos);
        model.addAttribute("usuario", (Usuario) session.getAttribute("usuario"));
        
        return "TelaExercicios";
    }

    @GetMapping("/provas")
    public String provas(HttpSession session, Model model) {
        if (!usuarioLogado(session)) return "redirect:/login";
        
        List<Conteudo> provasAnteriores = conteudoRepository.findByTipo(TipoConteudo.PROVA);
        model.addAttribute("provas", provasAnteriores);

        List<Calendario> eventos = calendarioRepository.findAll();
        model.addAttribute("eventos", eventos);
        model.addAttribute("usuario", (Usuario) session.getAttribute("usuario"));
        
        return "TelaProvas";
    }

    @GetMapping("/material")
    public String material(@RequestParam(value = "cat", required = false) String categoria, HttpSession session, Model model) {
        if (!usuarioLogado(session)) return "redirect:/login";

        List<Conteudo> materiaisDisponiveis = conteudoRepository.findByTipo(TipoConteudo.MATERIAL);
        
        if (categoria != null && !categoria.isEmpty() && materiaisDisponiveis != null) {
            materiaisDisponiveis = materiaisDisponiveis.stream()
                .filter(m -> categoria.equalsIgnoreCase(m.getNivel()))
                .toList();
            model.addAttribute("categoriaAtiva", categoria);
        }

        model.addAttribute("materiais", materiaisDisponiveis != null ? materiaisDisponiveis : new ArrayList<Conteudo>());

        List<Calendario> eventos = calendarioRepository.findAll();
        model.addAttribute("eventos", eventos);
        model.addAttribute("usuario", (Usuario) session.getAttribute("usuario"));
        
        return "TelaMaterialExtra";
    }

    // ================= AULAS POR MATÉRIA =================

    // ================= AULAS POR MATÉRIA =================

    @GetMapping("/aulas/matematica")
    public String aulasMatematica(@RequestParam("topico") String topico, Model model, HttpSession session) {
        if (!usuarioLogado(session)) return "redirect:/login";

        try {
            // Busca os conteúdos de MATEMATICA. Usamos o campo 'nivel' para salvar o nome do Tópico/Assunto
            List<Conteudo> todasAulas = conteudoRepository.findByDisciplinaAndNivel(Disciplina.MATEMATICA, topico);
            
            // Filtra via Stream para garantir que só exibe o tipo VIDEO
            List<Conteudo> aulasFiltradas = todasAulas != null ? todasAulas.stream()
                .filter(c -> TipoConteudo.VIDEO.equals(c.getTipo()))
                .toList() : new ArrayList<>();

            model.addAttribute("aulas", aulasFiltradas);
            model.addAttribute("tituloTopico", topico); // Para exibir o nome do assunto no topo da tela
            model.addAttribute("usuario", session.getAttribute("usuario"));
            
            return "TelaAulas";
        } catch (Exception e) {
            System.out.println("ERRO AO BUSCAR AULAS DE MATEMÁTICA: " + e.getMessage());
            return "redirect:/disciplinas";
        }
    }

    @GetMapping("/aulas/portugues")
    public String aulasPortugues(@RequestParam("topico") String topico, Model model, HttpSession session) {
        if (!usuarioLogado(session)) return "redirect:/login";

        try {
            // Busca os conteúdos de PORTUGUES filtrando pelo Tópico/Assunto (salvo em nivel)
            List<Conteudo> todasAulas = conteudoRepository.findByDisciplinaAndNivel(Disciplina.PORTUGUES, topico);
            
            // Filtra para trazer apenas os vídeos
            List<Conteudo> aulasFiltradas = todasAulas != null ? todasAulas.stream()
                .filter(c -> TipoConteudo.VIDEO.equals(c.getTipo()))
                .toList() : new ArrayList<>();

            model.addAttribute("aulas", aulasFiltradas);
            model.addAttribute("tituloTopico", topico);
            model.addAttribute("usuario", session.getAttribute("usuario"));
            
            return "TelaAulas";
        } catch (Exception e) {
            System.out.println("ERRO AO BUSCAR AULAS DE PORTUGUÊS: " + e.getMessage());
            return "redirect:/disciplinas";
        }
    }

    // ================= SISTEMA DO ADMIN =================

    @GetMapping("/login-admin")
    public String loginAdmin(HttpSession session) {
        if (adminLogado(session)) return "redirect:/admin";
        return "login-admin";
    }

    @PostMapping("/admin/autenticar")
    public String autenticarAdmin(@RequestParam String usuario, @RequestParam String senha, HttpSession session, Model model) {
        if (ADMIN_USER.equals(usuario) && ADMIN_PASS.equals(senha)) {
            session.setAttribute("isAdmin", true);
            return "redirect:/admin";
        }
        model.addAttribute("erro", "Credenciais de administrador inválidas!");
        return "login-admin";
    }

    @GetMapping("/admin")
    public String admin(HttpSession session) {
        if (!adminLogado(session)) return "redirect:/login-admin";
        return "admin-dashboard";
    }

    @GetMapping("/admin/conteudo")
    public String gerenciarAulas(HttpSession session, Model model) {
        if (!adminLogado(session)) return "redirect:/login-admin";
        List<Conteudo> aulas = conteudoRepository.findAll(); 
        model.addAttribute("aulas", aulas);
        return "gerenciar-aulas";
    }

    @GetMapping("/admin/upload")
    public String telaUpload(HttpSession session) {
        if (!adminLogado(session)) return "redirect:/login-admin";
        return "upload-conteudo";
    }

    @PostMapping("/salvarconteudo")
    public String salvarConteudo(@ModelAttribute Conteudo conteudo, HttpSession session) {
        if (!adminLogado(session)) return "redirect:/login-admin";
        conteudoRepository.save(conteudo); 
        return "redirect:/admin/conteudo";
    }

    @GetMapping("/admin/excluir/{id}")
    public String excluirConteudo(@PathVariable Long id, HttpSession session) {
        if (!adminLogado(session)) return "redirect:/login-admin";
        conteudoRepository.deleteById(id);
        return "redirect:/admin/conteudo";
    }

    @GetMapping("/admin/provas") 
    public String telaUploadProvas(HttpSession session) {
        if (!adminLogado(session)) return "redirect:/login-admin";
        return "upload-provas"; 
    }

    // ================= ROTAS DO ADMIN: GERENCIAR MATERIAIS EXTRAS =================

    @GetMapping("/admin/materiais")
    public String gerenciarMateriaisAdmin(HttpSession session, Model model) {
        if (!adminLogado(session)) return "redirect:/login-admin";
        
        List<Conteudo> materiais = conteudoRepository.findByTipo(TipoConteudo.MATERIAL);
        model.addAttribute("materiais", materiais != null ? materiais : new ArrayList<Conteudo>());
        return "MaterialExtraAdmin"; 
    }

    @PostMapping("/admin/materiais/salvar")
    public String adminSalvarMaterial(@ModelAttribute Conteudo conteudo, HttpSession session) {
        if (!adminLogado(session)) return "redirect:/login-admin";
        
        conteudo.setTipo(TipoConteudo.MATERIAL); 
        conteudoRepository.save(conteudo);
        return "redirect:/admin/materiais?sucesso=true";
    }

    @GetMapping("/admin/materiais/excluir/{id}")
    public String adminExcluirMaterial(@PathVariable Long id, HttpSession session) {
        if (!adminLogado(session)) return "redirect:/login-admin";
        
        conteudoRepository.deleteById(id);
        return "redirect:/admin/materiais";
    }

    // ================= GERENCIAMENTO DO CALENDÁRIO =================

    @GetMapping("/admin/calendario")
    public String gerenciarCalendario(HttpSession session, Model model){
        if (!adminLogado(session)) return "redirect:/login-admin";
        
        List<Calendario> eventos = calendarioRepository.findAll();
        model.addAttribute("eventos", eventos);
        return "gerenciar_calendario";
    }

    @PostMapping("/admin/calendario/salvar")
    public String salvarEvento(@ModelAttribute Calendario calendario, HttpSession session) {
        if (!adminLogado(session)) return "redirect:/login-admin";
        
        calendarioRepository.save(calendario);
        return "redirect:/admin/calendario?sucesso=true";
    }
    
    @GetMapping("/admin/calendario/excluir/{id}")
    public String excluirEvento(@PathVariable Long id, HttpSession session) {
        if (!adminLogado(session)) return "redirect:/login-admin";
        calendarioRepository.deleteById(id);
        return "redirect:/admin/calendario";
    }

    // ================= MÉTODOS AUXILIARES =================

    private boolean usuarioLogado(HttpSession session) {
        return session.getAttribute("usuario") != null;
    }

    private boolean adminLogado(HttpSession session) {
        return session.getAttribute("isAdmin") != null;
    }

    @GetMapping("/assistir/{id}")
    public String assistirAula(@PathVariable Long id, Model model, HttpSession session) {
        if (!usuarioLogado(session)) return "redirect:/login";
        
        Conteudo aula = conteudoRepository.findById(id).orElse(null);
        if (aula == null) return "redirect:/inicio"; 

        model.addAttribute("aula", aula);
        model.addAttribute("usuario", (Usuario) session.getAttribute("usuario"));
        return "TelaAssistir"; 
    }
}