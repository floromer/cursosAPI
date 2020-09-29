package ar.com.ada.api.cursos.controllers;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import ar.com.ada.api.cursos.entities.*;
import ar.com.ada.api.cursos.models.request.InscripcionRequest;
import ar.com.ada.api.cursos.models.response.CursoEstudianteResponse;
import ar.com.ada.api.cursos.models.response.DocenteSimplificadoResponse;
import ar.com.ada.api.cursos.models.response.GenericResponse;
import ar.com.ada.api.cursos.security.controllersSecurity.ControllersSecurity;
import ar.com.ada.api.cursos.services.*;

@RestController
public class EstudianteController {

    @Autowired
    EstudianteService estudianteService;

    @Autowired
    CursoService cursoService;

    @Autowired
    ControllersSecurity controllersSecurity;

    @PostMapping("/api/estudiantes")
    @PreAuthorize("@controllersSecurity.isStaff(principal)")
    public ResponseEntity<GenericResponse> crearEstudiante(@RequestBody Estudiante estudiante) {
        if (estudianteService.estudianteExiste(estudiante)) {
            return ResponseEntity.badRequest().body(new GenericResponse(false, "Este estudiante ya existe"));
        }

        estudianteService.crearEstudiante(estudiante);
        return ResponseEntity.ok(new GenericResponse(true, "Estudiante creada con éxito", estudiante.getEstudianteId()));
    }

    @PostMapping("/api/estudiantes/{id}/inscripciones")
    @PreAuthorize("@controllersSecurity.isEstudiante(principal, #id)")
    public ResponseEntity<GenericResponse> inscribir(@PathVariable Integer id, @RequestBody InscripcionRequest iR)
            throws Exception {
        Inscripcion inscripcionCreada = estudianteService.inscribir(id, iR.cursoId);
        if (inscripcionCreada == null) {
            return ResponseEntity.badRequest().body(new GenericResponse(false, "La inscripcion no pudo ser realizada"));
        } else {
            return ResponseEntity.ok(new GenericResponse(true, "La inscripción se realizó con éxito", inscripcionCreada.getInscripcionId()));
        }

    }

    @GetMapping("/api/estudiantes/{id}")
    @PreAuthorize("@controllersSecurity.isEstudiante(principal, #id) or @controllersSecurity.isStaff(principal)")
    ResponseEntity<Estudiante> buscarPorIdEstudiante(@PathVariable Integer id) throws Exception {
        Estudiante estudiante = estudianteService.buscarPorId(id);
        if (estudiante == null)
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(estudiante);
    }

    // con filtro sin cursos: /api/estudiantes?sinCursos=true
    // metodo para admin, obtener todos los estudiantes y todos los estudiantes que
    // no tienen cursos
    @GetMapping("/api/estudiantes")
    @PreAuthorize("@controllersSecurity.isStaff(principal) or @controllersSecurity.isDocente(principal, #id)")
    ResponseEntity<List<Estudiante>> listarEstudiantes(
            @RequestParam(value = "sinCursos", required = false) boolean sinCursos) {
        List<Estudiante> listaEstudiantes = new ArrayList<>();
        if (sinCursos) {
            listaEstudiantes = estudianteService.listaEstudianTesSinCurso();
        } else {
            listaEstudiantes = estudianteService.listaEstudiantes();
        }

        return ResponseEntity.ok(listaEstudiantes);
    }

    /*
     * - Estudiante -> Perspectiva estudiante: Ver cursos disponibles! (son todos
     * los cursos donde no este inscripto) - Estudiante -> ver mis cursos(solo
     * pueden verlo los estudiantes)
     * 
     * - /api/estudiantes/{id}/cursos?disponibles=true&categoria=1 o -
     * /api/estudiantes/{id}/cursos - /api/estudiantes/{id}/cursos/disponibles -> en
     * este caso es un metodo separado
     */
    @GetMapping("/api/estudiantes/{id}/cursos")
    @PreAuthorize("@controllersSecurity.isEstudiante(principal, #id)")
    public ResponseEntity<List<CursoEstudianteResponse>> listaCursos(@PathVariable Integer id,
            @RequestParam(value = "disponibles", required = false) boolean disponibles) throws Exception {
        List<Curso> listaCursos = new ArrayList<>();
        Estudiante estudiante = estudianteService.buscarPorId(id);
        if (disponibles) {
            listaCursos = cursoService.listaCursosDisponibles(estudiante);
        } else {
            listaCursos = estudiante.getCursosQueAsiste();
        }

        List<CursoEstudianteResponse> listaSimplificada = new ArrayList<>();
        for (Curso c : listaCursos) {
            CursoEstudianteResponse nuevoCurso = new CursoEstudianteResponse(c.getCursoId(), c.getNombre(), c.getDescripcion(), c.getDuracionHoras(), c.getCategorias());
            for (Docente d : c.getDocentes()) {
                DocenteSimplificadoResponse dr = new DocenteSimplificadoResponse(d.getDocenteId(), d.getNombre());
                nuevoCurso.docentes.add(dr);
            }
            listaSimplificada.add(nuevoCurso);
        }
        return ResponseEntity.ok(listaSimplificada);
    }
}
