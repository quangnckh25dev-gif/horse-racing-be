package com.horseracing.controller;

import com.horseracing.dto.RaceRefereeRequest;
import com.horseracing.dto.RaceRequest;
import com.horseracing.dto.RaceStatusRequest;
import com.horseracing.response.ApiResponse;
import com.horseracing.service.RaceRefereeService;
import com.horseracing.service.RaceService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin("*")
public class AdminRaceController {

    private final RaceService raceService;
    private final RaceRefereeService raceRefereeService;

    public AdminRaceController(RaceService raceService, RaceRefereeService raceRefereeService) {
        this.raceService = raceService;
        this.raceRefereeService = raceRefereeService;
    }

    @PostMapping("/races")
    public ApiResponse<?> createRace(@RequestBody RaceRequest request) {
        return ApiResponse.success(
                201,
                "Tao race thanh cong",
                raceService.createRace(request)
        );
    }

    @PutMapping("/races/{id}")
    public ApiResponse<?> updateRace(
            @PathVariable Integer id,
            @RequestBody RaceRequest request
    ) {
        return ApiResponse.success(
                200,
                "Cap nhat race thanh cong",
                raceService.updateRace(id, request)
        );
    }

    @DeleteMapping("/races/{id}")
    public ApiResponse<?> deleteRace(@PathVariable Integer id) {
        raceService.deleteRace(id);
        return ApiResponse.success(200, "Xoa race thanh cong", null);
    }

    @PatchMapping("/races/{id}/status")
    public ApiResponse<?> updateRaceStatus(
            @PathVariable Integer id,
            @RequestBody RaceStatusRequest request
    ) {
        return ApiResponse.success(
                200,
                "Cap nhat status race thanh cong",
                raceService.updateStatus(id, request.getStatus())
        );
    }

    @GetMapping("/referees")
    public ApiResponse<?> getReferees() {
        return ApiResponse.success(
                200,
                "Lay danh sach referee thanh cong",
                raceRefereeService.getReferees()
        );
    }

    @PostMapping("/races/{id}/referees")
    public ApiResponse<?> assignReferee(
            @PathVariable Integer id,
            @RequestBody RaceRefereeRequest request
    ) {
        return ApiResponse.success(
                201,
                "Phan cong referee thanh cong",
                raceRefereeService.assignReferee(id, request)
        );
    }

    @DeleteMapping("/races/{id}/referees/{refereeId}")
    public ApiResponse<?> removeReferee(
            @PathVariable Integer id,
            @PathVariable Integer refereeId
    ) {
        raceRefereeService.removeReferee(id, refereeId);
        return ApiResponse.success(200, "Huy phan cong referee thanh cong", null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleBadRequest(IllegalArgumentException ex) {
        return ApiResponse.error(400, ex.getMessage());
    }
}
