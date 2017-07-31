import { ComponentFixture, TestBed, async, inject } from '@angular/core/testing';
import { OnInit } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs/Rx';
import { DateUtils, DataUtils, EventManager } from 'ng-jhipster';
import { GreatBigExampleApplicationTestModule } from '../../../test.module';
import { MockActivatedRoute } from '../../../helpers/mock-route.service';
import { TalkDetailComponent } from '../../../../../../main/webapp/app/entities/talk/talk-detail.component';
import { TalkService } from '../../../../../../main/webapp/app/entities/talk/talk.service';
import { Talk } from '../../../../../../main/webapp/app/entities/talk/talk.model';

describe('Component Tests', () => {

    describe('Talk Management Detail Component', () => {
        let comp: TalkDetailComponent;
        let fixture: ComponentFixture<TalkDetailComponent>;
        let service: TalkService;

        beforeEach(async(() => {
            TestBed.configureTestingModule({
                imports: [GreatBigExampleApplicationTestModule],
                declarations: [TalkDetailComponent],
                providers: [
                    DateUtils,
                    DataUtils,
                    DatePipe,
                    {
                        provide: ActivatedRoute,
                        useValue: new MockActivatedRoute({id: 123})
                    },
                    TalkService,
                    EventManager
                ]
            }).overrideComponent(TalkDetailComponent, {
                set: {
                    template: ''
                }
            }).compileComponents();
        }));

        beforeEach(() => {
            fixture = TestBed.createComponent(TalkDetailComponent);
            comp = fixture.componentInstance;
            service = fixture.debugElement.injector.get(TalkService);
        });


        describe('OnInit', () => {
            it('Should call load all on init', () => {
            // GIVEN

            spyOn(service, 'find').and.returnValue(Observable.of(new Talk(10)));

            // WHEN
            comp.ngOnInit();

            // THEN
            expect(service.find).toHaveBeenCalledWith(123);
            expect(comp.talk).toEqual(jasmine.objectContaining({id:10}));
            });
        });
    });

});
